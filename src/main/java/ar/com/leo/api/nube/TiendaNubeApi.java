package ar.com.leo.api.nube;

import ar.com.leo.AppLogger;
import ar.com.leo.api.HttpRetryHandler;
import ar.com.leo.api.nube.model.NubeCredentials;
import ar.com.leo.api.nube.model.NubeCredentials.StoreCredentials;
import ar.com.leo.pickit.model.Venta;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static ar.com.leo.api.HttpRetryHandler.BASE_SECRET_DIR;

public class TiendaNubeApi {

    private static final ObjectMapper mapper = JsonMapper.shared();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Map<String, HttpRetryHandler> retryHandlers = new ConcurrentHashMap<>();
    private static final Path NUBE_CREDENTIALS_FILE = BASE_SECRET_DIR.resolve("nube_tokens.json");

    private static final String STORE_HOGAR = "KT HOGAR";
    private static final String STORE_GASTRO = "KT GASTRO";

    private static NubeCredentials credentials;

    public static boolean inicializar() {
        credentials = cargarCredenciales();
        if (credentials == null || credentials.stores == null || credentials.stores.isEmpty()) {
            AppLogger.warn("NUBE - No se encontraron credenciales de Tienda Nube.");
            return false;
        }
        return true;
    }

    public static List<Venta> obtenerVentasHogar() {
        StoreCredentials store = getStore(STORE_HOGAR);
        if (store == null) {
            AppLogger.warn("NUBE - Credenciales de " + STORE_HOGAR + " no disponibles.");
            return List.of();
        }
        return obtenerVentas(store, STORE_HOGAR);
    }

    public static List<Venta> obtenerVentasGastro() {
        StoreCredentials store = getStore(STORE_GASTRO);
        if (store == null) {
            AppLogger.warn("NUBE - Credenciales de " + STORE_GASTRO + " no disponibles.");
            return List.of();
        }
        return obtenerVentas(store, STORE_GASTRO);
    }

    private static List<Venta> obtenerVentas(StoreCredentials store, String label) {
        List<Venta> ventas = new ArrayList<>();
        String nextUrl = String.format(
                "https://api.tiendanube.com/v1/%s/orders?payment_status=paid&shipping_status=unpacked&status=open&aggregates=fulfillment_orders&per_page=200&page=1",
                store.storeId);

        while (nextUrl != null) {
            final String currentUrl = nextUrl;
            nextUrl = null;

            Supplier<HttpRequest> requestBuilder = () -> HttpRequest.newBuilder()
                    .uri(URI.create(currentUrl))
                    .header("Authentication", "bearer " + store.accessToken)
                    .header("User-Agent", "Pickit")
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = getRetryHandler(label).sendWithRetry(requestBuilder);

            if (response == null || response.statusCode() != 200) {
                if (response != null && response.statusCode() == 404 && response.body().contains("Last page is 0")) {
                    break;
                }
                String body = response != null ? response.body() : "sin respuesta";
                AppLogger.warn("NUBE (" + label + ") - Error al obtener órdenes: " + body);
                break;
            }

            JsonNode ordersArray = mapper.readTree(response.body());

            if (!ordersArray.isArray() || ordersArray.isEmpty()) {
                break;
            }

            for (JsonNode order : ordersArray) {
                long orderId = order.path("id").asLong(0);

                if (!tieneFulfillmentUnpacked(order)) continue;

                if (esPickup(order) && tieneNota(order)) {
                    AppLogger.info("NUBE (" + label + ") - Omitida orden pickup con nota: " + orderId);
                    continue;
                }

                JsonNode products = order.path("products");
                if (!products.isArray()) continue;

                for (JsonNode product : products) {
                    String sku = product.path("sku").asString("");
                    double quantity = product.path("quantity").asDouble(0);
                    String productName = product.path("name").asString("");

                    if (quantity <= 0) {
                        AppLogger.warn("NUBE (" + label + ") - Producto con cantidad inválida en orden " + orderId + ": " + sku);
                        String errorSku = sku.isBlank() ? productName : sku;
                        ventas.add(new Venta("CANT INVALIDA: " + errorSku, quantity, label));
                        continue;
                    }
                    if (sku.isBlank()) {
                        AppLogger.warn("NUBE (" + label + ") - Producto sin SKU en orden " + orderId + ": " + productName);
                        ventas.add(new Venta("SIN SKU: " + productName, quantity, label));
                        continue;
                    }
                    ventas.add(new Venta(sku, quantity, label));
                }
            }

            nextUrl = parseLinkNext(response);
        }

        AppLogger.info("NUBE (" + label + ") - Ventas obtenidas: " + ventas.size());
        return ventas;
    }

    private static String parseLinkNext(HttpResponse<String> response) {
        var linkHeader = response.headers().firstValue("Link").orElse(null);
        if (linkHeader == null) return null;

        for (String part : linkHeader.split(",")) {
            part = part.trim();
            if (part.contains("rel=\"next\"")) {
                int start = part.indexOf('<');
                int end = part.indexOf('>');
                if (start >= 0 && end > start) {
                    return part.substring(start + 1, end);
                }
            }
        }
        return null;
    }

    private static boolean esPickup(JsonNode order) {
        JsonNode fulfillments = order.path("fulfillments");
        if (!fulfillments.isArray()) return false;
        for (JsonNode fo : fulfillments) {
            if ("pickup".equalsIgnoreCase(fo.path("shipping").path("type").asString(""))) {
                return true;
            }
        }
        return false;
    }

    private static boolean tieneNota(JsonNode order) {
        String nota = order.path("owner_note").asString("").trim();
        return !nota.isEmpty();
    }

    private static boolean tieneFulfillmentUnpacked(JsonNode order) {
        JsonNode fulfillments = order.path("fulfillments");
        if (!fulfillments.isArray() || fulfillments.isEmpty()) return false;
        for (JsonNode fo : fulfillments) {
            if ("unpacked".equalsIgnoreCase(fo.path("status").asString(""))) {
                return true;
            }
        }
        return false;
    }

    private static StoreCredentials getStore(String storeName) {
        if (credentials == null || credentials.stores == null) return null;
        return credentials.stores.get(storeName);
    }

    private static HttpRetryHandler getRetryHandler(String storeName) {
        return retryHandlers.computeIfAbsent(storeName, k -> new HttpRetryHandler(httpClient, 10000L, 2));
    }

    private static NubeCredentials cargarCredenciales() {
        try {
            File f = NUBE_CREDENTIALS_FILE.toFile();
            if (!f.exists()) return null;
            return mapper.readValue(f, NubeCredentials.class);
        } catch (Exception e) {
            AppLogger.warn("NUBE - Error al cargar credenciales: " + e.getMessage());
            return null;
        }
    }
}
