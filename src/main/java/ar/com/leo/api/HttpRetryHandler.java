package ar.com.leo.api;

import ar.com.leo.AppLogger;
import com.google.common.util.concurrent.RateLimiter;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;


public class HttpRetryHandler {

    public static final Path BASE_SECRET_DIR = Paths.get(
            System.getenv("PROGRAMDATA") != null ? System.getenv("PROGRAMDATA") : System.getProperty("java.io.tmpdir"),
            "SuperMaster", "secrets");
    private static final int MAX_RETRIES = 3;
    private static final int MAX_RETRIES_RATE_LIMIT = 5;
    private static final int MAX_RETRIES_AUTH = 2;
    private static final long MAX_WAIT_MS = 300000;
    private final long BASE_WAIT_MS;
    private final RateLimiter rateLimiter;
    private final Runnable onAuthError;

    private final HttpClient client;

    public HttpRetryHandler(HttpClient client, long BASE_WAIT_MS, double permitsPerSecond) {
        this(client, BASE_WAIT_MS, permitsPerSecond, null);
    }

    public HttpRetryHandler(HttpClient client, long BASE_WAIT_MS, double permitsPerSecond, Runnable onAuthError) {
        this.client = client;
        this.BASE_WAIT_MS = BASE_WAIT_MS;
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
        this.onAuthError = onAuthError;
    }

    public HttpResponse<String> sendWithRetry(Supplier<HttpRequest> requestSupplier) {
        HttpResponse<String> response = null;
        int authRetries = 0;
        int rateLimitRetries = 0;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rateLimiter.acquire();

                HttpRequest request = requestSupplier.get(); // request actualizado
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                // ---- OK ----
                if (status >= 200 && status < 300)
                    return response;

                // ---- Token expirado ----
                if (status == 401) {
                    if (onAuthError == null) {
                        AppLogger.warn("401 Unauthorized - Sin handler de autenticación configurado.");
                        return response;
                    }
                    authRetries++;
                    if (authRetries > MAX_RETRIES_AUTH) {
                        AppLogger.error("401 Unauthorized - Máximo de reintentos de autenticación alcanzado ("
                                + authRetries + ")", null);
                        return response;
                    }
                    AppLogger.warn("401 Unauthorized → actualizando tokens... (intento " + authRetries + "/"
                            + MAX_RETRIES_AUTH + ")");
                    onAuthError.run();
                    continue;
                }

                // ---- Error de concurrencia ----
                if (status == 409 || status == 423) {
                    if (attempt >= MAX_RETRIES) {
                        AppLogger.error("409/423 Conflict - Máximo de reintentos alcanzado (" + attempt + ")", null);
                        return response;
                    }
                    long waitMs = BASE_WAIT_MS + ThreadLocalRandom.current().nextInt(200, 800);
                    AppLogger.warn("409/423 Conflict (KVS). Retry en " + waitMs + " ms... (intento " + attempt + "/"
                            + MAX_RETRIES + ")");
                    Thread.sleep(waitMs);
                    continue;
                }

                // ---- Too Many Requests ----
                if (status == 429) {
                    rateLimitRetries++;
                    if (rateLimitRetries > MAX_RETRIES_RATE_LIMIT) {
                        AppLogger.error("429 Too Many Requests - Máximo de reintentos de rate limit alcanzado ("
                                + rateLimitRetries + ")", null);
                        return response;
                    }
                    long waitMs = parseRetryAfter(response, BASE_WAIT_MS * (long) Math.pow(2, rateLimitRetries));
                    // Limitar el tiempo de espera máximo
                    waitMs = Math.min(waitMs, MAX_WAIT_MS);
                    AppLogger.warn("429 Too Many Requests. Retry en " + (waitMs / 1000) + " segundos... (intento "
                            + rateLimitRetries + "/" + MAX_RETRIES_RATE_LIMIT + ")");
                    Thread.sleep(waitMs);
                    // No contar como intento normal, continuar
                    attempt--; // No contar este como intento normal
                    continue;
                }

                // ---- Errores de servidor ----
                if (status >= 500 && status < 600) {
                    if (attempt >= MAX_RETRIES) {
                        AppLogger.error("5xx Error - Máximo de reintentos alcanzado (" + attempt + ")", null);
                        return response;
                    }
                    long waitMs = BASE_WAIT_MS * (long) Math.pow(2, attempt - 1);
                    AppLogger.warn(
                            "5xx Error. Retry en " + waitMs + " ms... (intento " + attempt + "/" + MAX_RETRIES + ")");
                    Thread.sleep(waitMs);
                    continue;
                }

                // ---- Errores 400-499 no recuperables ----
                return response;

            } catch (IOException e) {
                long waitMs = BASE_WAIT_MS * (long) Math.pow(2, attempt - 1);
                AppLogger.warn("IOException. Retry en " + waitMs + " ms... (" + attempt + "/" + MAX_RETRIES + ")");
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return response;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return response;
            }
        }

        return response;
    }

    private long parseRetryAfter(HttpResponse<String> response, long defaultMs) {
        return response.headers().firstValue("Retry-After").map(value -> {
            try {
                // si es número → segundos
                return Long.parseLong(value) * 1000;
            } catch (NumberFormatException e) {
                try {
                    // si es fecha → calcular diferencia
                    long epoch = java.time.ZonedDateTime
                            .parse(value, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                            .toEpochMilli();

                    return Math.max(epoch - System.currentTimeMillis(), defaultMs);
                } catch (Exception ignored2) {
                    return defaultMs;
                }
            }
        }).orElse(defaultMs);
    }

}
