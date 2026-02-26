package ar.com.leo.pickit.api;

import ar.com.leo.AppLogger;
import ar.com.leo.api.HttpRetryHandler;
import ar.com.leo.api.ml.MercadoLibreAPI;
import ar.com.leo.pickit.model.OrdenML;
import ar.com.leo.pickit.model.Venta;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class PickitMercadoLibreAPI {

    public record MLOrderResult(List<Venta> ventas, List<OrdenML> ordenes) {}
    public record SlaInfo(String status, OffsetDateTime expectedDate) {}

    private static final ObjectMapper mapper = JsonMapper.shared();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final HttpRetryHandler retryHandler = new HttpRetryHandler(httpClient, 30000L, 5, MercadoLibreAPI::verificarTokens);

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

    public static MLOrderResult obtenerVentasReadyToPrint(String userId) {
        verificarTokens();

        List<Venta> ventas = new ArrayList<>();
        List<OrdenML> ordenes = new ArrayList<>();
        Set<Long> orderIdsSeen = new HashSet<>();
        int offset = 0;
        final int limit = 50;
        boolean hasMore = true;

        while (hasMore) {
            final int currentOffset = offset;
            String url = String.format(
                    "https://api.mercadolibre.com/orders/search?seller=%s&shipping.status=ready_to_ship&shipping.substatus=ready_to_print&sort=date_asc&offset=%d&limit=%d",
                    userId, currentOffset, limit);

            Supplier<HttpRequest> requestBuilder = () -> HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + getToken())
                    .GET()
                    .build();

            HttpResponse<String> response = retryHandler.sendWithRetry(requestBuilder);

            if (response == null || response.statusCode() != 200) {
                String body = response != null ? response.body() : "sin respuesta";
                AppLogger.warn("ML - Error al obtener órdenes ready_to_print (offset " + currentOffset + "): " + body);
                break;
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode results = root.path("results");

            if (!results.isArray() || results.isEmpty()) break;

            for (JsonNode order : results) {
                long orderId = order.path("id").asLong();
                if (!orderIdsSeen.add(orderId)) continue;

                JsonNode tagsNode = order.path("tags");
                if (tagsNode.isArray()) {
                    boolean esEntregada = false;
                    for (JsonNode tag : tagsNode) {
                        if ("delivered".equals(tag.asString())) { esEntregada = true; break; }
                    }
                    if (esEntregada) continue;
                }

                OffsetDateTime fecha = parseFecha(order.path("date_created").asString(""));
                JsonNode packNode = order.path("pack_id");
                Long packId = packNode.isNull() || packNode.isMissingNode() ? null : packNode.asLong();
                JsonNode shippingNode = order.path("shipping").path("id");
                Long shipmentId = shippingNode.isNull() || shippingNode.isMissingNode() ? null : shippingNode.asLong();
                OrdenML ordenML = new OrdenML(orderId, packId, shipmentId, fecha);

                JsonNode orderItems = order.path("order_items");
                if (!orderItems.isArray()) continue;

                for (JsonNode orderItem : orderItems) {
                    JsonNode item = orderItem.path("item");
                    String sku = item.path("seller_sku").asString("");
                    if (sku.isBlank()) sku = item.path("seller_custom_field").asString("");
                    String itemTitle = item.path("title").asString("");
                    double quantity = orderItem.path("quantity").asDouble(0);

                    if (quantity <= 0) {
                        String errorSku = sku.isBlank() ? itemTitle : sku;
                        Venta venta = new Venta("CANT INVALIDA: " + errorSku, quantity, "ML");
                        ventas.add(venta);
                        ordenML.getItems().add(venta);
                        continue;
                    }
                    if (sku.isBlank()) {
                        Venta venta = new Venta("SIN SKU: " + itemTitle, quantity, "ML");
                        ventas.add(venta);
                        ordenML.getItems().add(venta);
                        continue;
                    }
                    Venta venta = new Venta(sku, quantity, "ML");
                    ventas.add(venta);
                    ordenML.getItems().add(venta);
                }

                if (!ordenML.getItems().isEmpty()) ordenes.add(ordenML);
            }

            JsonNode paging = root.path("paging");
            int total = paging.path("total").asInt(0);
            offset += limit;
            hasMore = offset < total;
            AppLogger.info(String.format("ML - Obtenidas %d/%d órdenes ready_to_print", Math.min(offset, total), total));
        }

        AppLogger.info("ML - Ventas ready_to_print: " + ventas.size());
        return new MLOrderResult(ventas, ordenes);
    }

    public static MLOrderResult obtenerVentasSellerAgreement(String userId) {
        verificarTokens();

        List<Venta> ventas = new ArrayList<>();
        List<OrdenML> ordenes = new ArrayList<>();
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
                AppLogger.warn("ML - Error al obtener órdenes seller_agreement (offset " + currentOffset + "): " + body);
                break;
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode results = root.path("results");

            if (!results.isArray() || results.isEmpty()) break;

            for (JsonNode order : results) {
                long orderId = order.path("id").asLong();
                if (!orderIdsSeen.add(orderId)) continue;

                JsonNode tagsNode = order.path("tags");
                if (tagsNode.isArray()) {
                    boolean esEntregada = false;
                    for (JsonNode tag : tagsNode) {
                        if ("delivered".equals(tag.asString())) { esEntregada = true; break; }
                    }
                    if (esEntregada) continue;
                }

                if (order.path("fulfilled").asBoolean(false)) continue;

                if (tieneNota(orderId)) {
                    omitidas++;
                    continue;
                }

                OffsetDateTime fecha = parseFecha(order.path("date_created").asString(""));
                JsonNode packNode = order.path("pack_id");
                Long packId = packNode.isNull() || packNode.isMissingNode() ? null : packNode.asLong();
                OrdenML ordenML = new OrdenML(orderId, packId, null, fecha);

                JsonNode orderItems = order.path("order_items");
                if (!orderItems.isArray()) continue;

                for (JsonNode orderItem : orderItems) {
                    JsonNode item = orderItem.path("item");
                    String sku = item.path("seller_sku").asString("");
                    if (sku.isBlank()) sku = item.path("seller_custom_field").asString("");
                    String itemTitle = item.path("title").asString("");
                    double quantity = orderItem.path("quantity").asDouble(0);

                    if (quantity <= 0) {
                        String errorSku = sku.isBlank() ? itemTitle : sku;
                        Venta venta = new Venta("CANT INVALIDA: " + errorSku, quantity, "ML Acuerdo");
                        ventas.add(venta);
                        ordenML.getItems().add(venta);
                        continue;
                    }
                    if (sku.isBlank()) {
                        Venta venta = new Venta("SIN SKU: " + itemTitle, quantity, "ML Acuerdo");
                        ventas.add(venta);
                        ordenML.getItems().add(venta);
                        continue;
                    }
                    Venta venta = new Venta(sku, quantity, "ML Acuerdo");
                    ventas.add(venta);
                    ordenML.getItems().add(venta);
                }

                if (!ordenML.getItems().isEmpty()) ordenes.add(ordenML);
            }

            JsonNode paging = root.path("paging");
            int total = paging.path("total").asInt(0);
            offset += limit;
            hasMore = offset < total;
            AppLogger.info(String.format("ML - Obtenidas %d/%d órdenes seller_agreement", Math.min(offset, total), total));
        }

        AppLogger.info("ML - Ventas seller_agreement: " + ventas.size() + " (omitidas con nota: " + omitidas + ")");
        return new MLOrderResult(ventas, ordenes);
    }

    public static SlaInfo obtenerSla(long shipmentId) {
        verificarTokens();

        String url = "https://api.mercadolibre.com/shipments/" + shipmentId + "/sla";

        Supplier<HttpRequest> requestBuilder = () -> HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getToken())
                .GET()
                .build();

        HttpResponse<String> response = retryHandler.sendWithRetry(requestBuilder);

        if (response == null || response.statusCode() != 200) {
            AppLogger.warn("ML - Error al obtener SLA de shipment " + shipmentId);
            return null;
        }

        try {
            JsonNode root = mapper.readTree(response.body());
            String status = root.path("status").asString("");
            String expectedDateStr = root.path("expected_date").asString("");
            OffsetDateTime expectedDate = null;
            if (!expectedDateStr.isBlank()) {
                try { expectedDate = OffsetDateTime.parse(expectedDateStr); } catch (Exception ignored) {}
            }
            return new SlaInfo(status, expectedDate);
        } catch (Exception e) {
            AppLogger.warn("ML - Error al leer SLA de shipment " + shipmentId + ": " + e.getMessage());
            return null;
        }
    }

    public static Map<Long, SlaInfo> obtenerSlasParalelo(List<Long> shipmentIds) {
        Map<Long, SlaInfo> slaMap = new LinkedHashMap<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Long shipmentId : shipmentIds) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                SlaInfo sla = obtenerSla(shipmentId);
                if (sla != null) {
                    synchronized (slaMap) {
                        slaMap.put(shipmentId, sla);
                    }
                }
            });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return slaMap;
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
            AppLogger.warn("ML - Error al leer notas de orden " + orderId + ": " + e.getMessage());
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
