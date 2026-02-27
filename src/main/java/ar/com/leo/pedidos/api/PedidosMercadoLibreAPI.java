package ar.com.leo.pedidos.api;

import ar.com.leo.AppLogger;
import ar.com.leo.api.HttpRetryHandler;
import ar.com.leo.api.ml.MercadoLibreAPI;
import ar.com.leo.pedidos.model.PedidoML;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Supplier;

public class PedidosMercadoLibreAPI {

    private static final ObjectMapper mapper = JsonMapper.shared();
    private static final HttpRetryHandler retryHandler = MercadoLibreAPI.getRetryHandler();

    public static boolean inicializar() {
        return MercadoLibreAPI.inicializar();
    }

    public static void verificarTokens() {
        MercadoLibreAPI.verificarTokens();
    }

    private static String getToken() {
        return MercadoLibreAPI.getAccessToken();
    }

    public static String getUserId() throws IOException {
        verificarTokens();
        final String url = "https://api.mercadolibre.com/users/me";

        Supplier<HttpRequest> requestBuilder = () -> HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getToken())
                .GET()
                .build();

        HttpResponse<String> response = retryHandler.sendWithRetry(requestBuilder);

        if (response == null || response.statusCode() != 200) {
            throw new IOException("Error al obtener el user ID de ML: " + (response != null ? response.body() : "sin respuesta"));
        }

        return mapper.readTree(response.body()).get("id").asString();
    }

    public static List<PedidoML> obtenerPedidosRetiro(String userId) {
        verificarTokens();

        List<PedidoML> pedidos = new ArrayList<>();
        Set<Long> orderIdsSeen = new HashSet<>();
        int offset = 0;
        final int limit = 50;
        boolean hasMore = true;
        int omitidas = 0;

        while (hasMore) {
            final int currentOffset = offset;
            String fechaDesde = OffsetDateTime.now()
                    .minusDays(7)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00:00.000XXX"));

            String url = String.format(
                    "https://api.mercadolibre.com/orders/search?seller=%s&tags=no_shipping&order.status=paid&order.date_created.from=%s&sort=date_asc&offset=%d&limit=%d",
                    userId, URLEncoder.encode(fechaDesde, StandardCharsets.UTF_8), currentOffset, limit);

            Supplier<HttpRequest> requestBuilder = () -> HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + getToken())
                    .GET()
                    .build();

            HttpResponse<String> response = retryHandler.sendWithRetry(requestBuilder);

            if (response == null || response.statusCode() != 200) {
                String body = response != null ? response.body() : "sin respuesta";
                AppLogger.warn("PEDIDOS ML - Error al obtener órdenes retiro (offset " + currentOffset + "): " + body);
                break;
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode results = root.path("results");

            if (!results.isArray() || results.isEmpty()) break;

            for (JsonNode order : results) {
                long orderId = order.path("id").asLong();
                if (!orderIdsSeen.add(orderId)) continue;

                // Skip delivered
                JsonNode tagsNode = order.path("tags");
                if (tagsNode.isArray()) {
                    boolean esEntregada = false;
                    for (JsonNode tag : tagsNode) {
                        if ("delivered".equals(tag.asString())) { esEntregada = true; break; }
                    }
                    if (esEntregada) continue;
                }

                // Skip fulfilled
                if (order.path("fulfilled").asBoolean(false)) continue;

                // Skip con nota
                if (tieneNota(orderId)) {
                    omitidas++;
                    continue;
                }

                OffsetDateTime fecha = parseFecha(order.path("date_created").asString(""));

                // Buyer info
                JsonNode buyer = order.path("buyer");
                String usuario = buyer.path("nickname").asString("");
                String firstName = buyer.path("first_name").asString("");
                String lastName = buyer.path("last_name").asString("");
                String nombreApellido = (firstName + " " + lastName).trim();

                // Items
                JsonNode orderItems = order.path("order_items");
                if (!orderItems.isArray()) continue;

                for (JsonNode orderItem : orderItems) {
                    JsonNode item = orderItem.path("item");
                    String sku = item.path("seller_sku").asString("");
                    if (sku.isBlank()) sku = item.path("seller_custom_field").asString("");
                    String detalle = item.path("title").asString("");
                    double quantity = orderItem.path("quantity").asDouble(0);

                    pedidos.add(new PedidoML(orderId, fecha, usuario, nombreApellido, sku, quantity, detalle));
                }
            }

            JsonNode paging = root.path("paging");
            int total = paging.path("total").asInt(0);
            offset += limit;
            hasMore = offset < total;
            AppLogger.info(String.format("PEDIDOS ML - Obtenidas %d/%d órdenes retiro", Math.min(offset, total), total));
        }

        AppLogger.info("PEDIDOS ML - Pedidos retiro: " + pedidos.size() + " (omitidas con nota: " + omitidas + ")");
        return pedidos;
    }

    private static boolean tieneNota(long orderId) {
        Supplier<HttpRequest> requestBuilder = () -> HttpRequest.newBuilder()
                .uri(URI.create("https://api.mercadolibre.com/orders/" + orderId + "/notes"))
                .header("Authorization", "Bearer " + getToken())
                .GET()
                .build();

        HttpResponse<String> response = retryHandler.sendWithRetry(requestBuilder);

        if (response == null || response.statusCode() != 200) return false;

        try {
            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) return false;
            JsonNode results = root.get(0).path("results");
            if (!results.isArray()) return false;
            for (JsonNode note : results) {
                String texto = note.path("note").asString("").trim();
                if (!texto.isEmpty()) return true;
            }
        } catch (Exception e) {
            AppLogger.warn("PEDIDOS ML - Error al leer notas de orden " + orderId + ": " + e.getMessage());
        }
        return false;
    }

    private static OffsetDateTime parseFecha(String dateCreated) {
        if (dateCreated == null || dateCreated.isBlank()) return null;
        try {
            return OffsetDateTime.parse(dateCreated);
        } catch (Exception e) {
            return null;
        }
    }
}
