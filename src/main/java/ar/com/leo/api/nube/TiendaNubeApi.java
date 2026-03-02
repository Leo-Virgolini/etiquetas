package ar.com.leo.api.nube;

import ar.com.leo.AppLogger;
import ar.com.leo.api.HttpRetryHandler;
import ar.com.leo.api.ml.model.Venta;
import ar.com.leo.api.nube.model.NubeCredentials;
import ar.com.leo.api.nube.model.NubeCredentials.StoreCredentials;
import ar.com.leo.pedidos.model.EtiquetaTN;
import ar.com.leo.pedidos.model.PedidoTN;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static ar.com.leo.api.HttpRetryHandler.BASE_SECRET_DIR;

public class TiendaNubeApi {

    public record TiendaNubeOrderResult(List<PedidoTN> pedidos, List<EtiquetaTN> etiquetas, int totalOrdenes) {
    }

    public record VentasResult(List<Venta> ventas, int totalOrdenes) {
    }

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

    public static VentasResult obtenerVentasHogar() {
        StoreCredentials store = getStore(STORE_HOGAR);
        if (store == null) {
            AppLogger.warn("NUBE - Credenciales de " + STORE_HOGAR + " no disponibles.");
            return new VentasResult(List.of(), 0);
        }
        return obtenerVentas(store, STORE_HOGAR);
    }

    public static VentasResult obtenerVentasGastro() {
        StoreCredentials store = getStore(STORE_GASTRO);
        if (store == null) {
            AppLogger.warn("NUBE - Credenciales de " + STORE_GASTRO + " no disponibles.");
            return new VentasResult(List.of(), 0);
        }
        return obtenerVentas(store, STORE_GASTRO);
    }

    private static VentasResult obtenerVentas(StoreCredentials store, String label) {
        List<Venta> ventas = new ArrayList<>();
        int totalOrdenes = 0;
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
                long orderNumber = order.path("number").asLong(0);

                if (!tieneFulfillmentUnpacked(order)) continue;

                if (esPickup(order) && tieneNota(order)) {
                    AppLogger.info("NUBE (" + label + ") - Omitida orden pickup con nota: " + orderNumber);
                    continue;
                }

                totalOrdenes++;

                JsonNode products = order.path("products");
                if (!products.isArray()) continue;

                for (JsonNode product : products) {
                    String sku = product.path("sku").asString("");
                    double quantity = product.path("quantity").asDouble(0);
                    String productName = product.path("name").asString("");

                    if (quantity <= 0) {
                        AppLogger.warn("NUBE (" + label + ") - Producto con cantidad inválida en orden " + orderNumber + ": " + sku);
                        String errorSku = sku.isBlank() ? productName : sku;
                        ventas.add(new Venta("CANT INVALIDA: " + errorSku, quantity, label));
                        continue;
                    }
                    if (sku.isBlank()) {
                        AppLogger.warn("NUBE (" + label + ") - Producto sin SKU en orden " + orderNumber + ": " + productName);
                        ventas.add(new Venta("SIN SKU: " + productName, quantity, label));
                        continue;
                    }
                    ventas.add(new Venta(sku, quantity, label));
                }
            }

            nextUrl = parseLinkNext(response);
        }

        AppLogger.info("NUBE (" + label + ") - Órdenes: " + totalOrdenes + " | Productos: " + ventas.size());
        return new VentasResult(ventas, totalOrdenes);
    }

    // ── Pedidos completos (para tab Pedidos) ──

    public static TiendaNubeOrderResult obtenerPedidosCompletosHogar() {
        StoreCredentials store = getStore(STORE_HOGAR);
        if (store == null) {
            AppLogger.warn("NUBE - Credenciales de " + STORE_HOGAR + " no disponibles.");
            return new TiendaNubeOrderResult(List.of(), List.of(), 0);
        }
        return obtenerPedidosCompletos(store, STORE_HOGAR);
    }

    public static TiendaNubeOrderResult obtenerPedidosCompletosGastro() {
        StoreCredentials store = getStore(STORE_GASTRO);
        if (store == null) {
            AppLogger.warn("NUBE - Credenciales de " + STORE_GASTRO + " no disponibles.");
            return new TiendaNubeOrderResult(List.of(), List.of(), 0);
        }
        return obtenerPedidosCompletos(store, STORE_GASTRO);
    }

    private static TiendaNubeOrderResult obtenerPedidosCompletos(StoreCredentials store, String label) {
        List<PedidoTN> pedidos = new ArrayList<>();
        List<EtiquetaTN> etiquetas = new ArrayList<>();
        int totalOrdenes = 0;

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
                AppLogger.warn("PEDIDOS NUBE (" + label + ") - Error al obtener órdenes: " + body);
                break;
            }

            JsonNode ordersArray = mapper.readTree(response.body());

            if (!ordersArray.isArray() || ordersArray.isEmpty()) {
                break;
            }

            for (JsonNode order : ordersArray) {
                long orderNumber = order.path("number").asLong(0);

                if (!tieneFulfillmentUnpacked(order)) continue;

                if (esPickup(order) && tieneNota(order)) {
                    AppLogger.info("PEDIDOS NUBE (" + label + ") - Omitida orden pickup con nota: " + orderNumber);
                    continue;
                }

                // Datos comunes de la orden (campos según API TN: contact_name, created_at)
                String customerName = order.path("contact_name").asString("");
                OffsetDateTime fecha = parseFechaISO(order.path("created_at").asString(""));
                String ownerNote = order.path("owner_note").asString("").trim();

                // Detectar método de envío (priorizar nombre real de la API)
                String shippingName = obtenerNombreEnvio(order);
                String tipoEnvio;
                String snUpper = shippingName != null ? shippingName.toUpperCase() : "";
                if (shippingName != null && shippingName.equalsIgnoreCase("Local del Vendedor")) {
                    tipoEnvio = "RETIRO";
                } else if (esPickup(order) && !snUpper.contains("CORREO") && !snUpper.contains("ENVÍO NUBE") && !snUpper.contains("ENVIO NUBE")) {
                    tipoEnvio = "RETIRO";
                } else if (snUpper.contains("LLEGA HOY")) {
                    // Quitar detalle entre paréntesis y normalizar a "LLEGA HOY - ZONA"
                    int parenIdx = shippingName.indexOf('(');
                    String clean = (parenIdx > 0 ? shippingName.substring(0, parenIdx).trim() : shippingName.trim()).toUpperCase();
                    // "CABA - LLEGA HOY" → "LLEGA HOY - CABA"
                    if (clean.contains(" - ") && !clean.startsWith("LLEGA HOY")) {
                        String[] parts = clean.split("\\s*-\\s*", 2);
                        clean = parts[1] + " - " + parts[0];
                    }
                    tipoEnvio = clean;
                } else {
                    tipoEnvio = "CORREO";
                }
                AppLogger.info("PEDIDOS NUBE (" + label + ") - Orden #" + orderNumber + " | Envío: " + tipoEnvio);

                totalOrdenes++;

                // Productos -> PedidoTN (uno por producto)
                JsonNode products = order.path("products");
                if (products.isArray()) {
                    for (JsonNode product : products) {
                        String sku = product.path("sku").asString("");
                        double quantity = product.path("quantity").asDouble(0);
                        String productName = product.path("name").asString("");

                        pedidos.add(new PedidoTN(orderNumber, fecha, customerName, sku, quantity, productName, label, tipoEnvio));
                    }
                }

                if (shippingName != null
                        && shippingName.toUpperCase().contains("LLEGA HOY")
                        && !shippingName.toUpperCase().contains("ZIPPIN")) {

                    JsonNode shippingAddress = order.path("shipping_address");
                    String domicilio = buildDomicilio(shippingAddress);
                    String localidad = buildLocalidad(shippingAddress);
                    String cp = shippingAddress.path("zipcode").asString("");
                    String telefono = shippingAddress.path("phone").asString("");
                    if (telefono.isBlank()) {
                        telefono = order.path("contact_phone").asString("");
                    }

                    etiquetas.add(new EtiquetaTN(orderNumber, fecha, customerName,
                            domicilio, localidad, cp, telefono, ownerNote));
                }
            }

            nextUrl = parseLinkNext(response);
        }

        AppLogger.info("PEDIDOS NUBE (" + label + ") - Órdenes: " + totalOrdenes + " | Etiquetas LLEGA HOY: " + etiquetas.size());
        return new TiendaNubeOrderResult(pedidos, etiquetas, totalOrdenes);
    }

    private static String buildLocalidad(JsonNode shippingAddress) {
        String locality = shippingAddress.path("locality").asString("").trim();
        String city = shippingAddress.path("city").asString("").trim();

        if (!locality.isBlank() && !city.isBlank()) {
            return locality + ", " + city;
        }
        return city.isBlank() ? locality : city;
    }

    private static String obtenerNombreEnvio(JsonNode order) {
        // Priorizar fulfillments: carrier.name + option.name
        JsonNode fulfillments = order.path("fulfillments");
        if (fulfillments.isArray()) {
            for (JsonNode fo : fulfillments) {
                JsonNode shipping = fo.path("shipping");
                String optionName = shipping.path("option").path("name").asString("");
                if (optionName.isBlank()) continue;

                String carrierName = shipping.path("carrier").path("name").asString("");
                if (!carrierName.isBlank() && !optionName.toLowerCase().contains(carrierName.toLowerCase())) {
                    return carrierName + " - " + optionName;
                }
                return optionName;
            }
        }

        // Fallback: shipping_option a nivel de orden (puede ser string u objeto con "name")
        JsonNode shippingOption = order.path("shipping_option");
        if (!shippingOption.isMissingNode() && !shippingOption.isNull()) {
            if (shippingOption.isString()) {
                String name = shippingOption.asString("");
                if (!name.isBlank()) return name;
            } else {
                String name = shippingOption.path("name").asString("");
                if (!name.isBlank()) return name;
            }
        }

        return null;
    }

    private static String buildDomicilio(JsonNode shippingAddress) {
        String calle = shippingAddress.path("address").asString("").trim();
        String numero = shippingAddress.path("number").asString("").trim();
        String piso = shippingAddress.path("floor").asString("").trim();

        StringBuilder sb = new StringBuilder();
        if (!calle.isBlank()) sb.append(calle);
        if (!numero.isBlank()) sb.append(" ").append(numero);
        if (!piso.isBlank()) sb.append(" - ").append(piso);
        return sb.toString().trim();
    }

    private static OffsetDateTime parseFechaISO(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            // TN devuelve "+0000" sin dos puntos, OffsetDateTime espera "+00:00"
            if (dateStr.matches(".*[+-]\\d{4}$")) {
                dateStr = dateStr.substring(0, dateStr.length() - 2) + ":" + dateStr.substring(dateStr.length() - 2);
            }
            return OffsetDateTime.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
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

    /**
     * Detecta si la orden es de retiro en local (pickup) usando los fulfillments.
     * <p>
     * Órdenes nuevas: tienen array {@code fulfillments} con {@code shipping.type = "pickup"}.
     * Órdenes viejas: no tienen fulfillments (array vacío/ausente) y usaban
     * {@code shipping_pickup_type} a nivel de orden. Se descartan retornando false
     * para evitar procesar órdenes antiguas sin datos de envío confiables.
     */
    private static boolean esPickup(JsonNode order) {
        JsonNode fulfillments = order.path("fulfillments");
        if (!fulfillments.isArray() || fulfillments.isEmpty()) return false;
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

    /**
     * Verifica si una orden tiene al menos un fulfillment con status UNPACKED.
     * <p>
     * Órdenes nuevas: tienen array {@code fulfillments} con {@code status = "unpacked"}.
     * Órdenes viejas: no tienen fulfillments (array vacío/ausente) y usaban
     * {@code shipping_status} a nivel de orden. Se descartan retornando false
     * para evitar procesar órdenes antiguas sin SKU ni datos confiables.
     */
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

    // ── Test: consultar una orden por número y mostrar datos de envío ──
    // Uso: main("12345") o main("12345", "67890")
    public static void main(String[] args) throws Exception {
        if (!inicializar()) {
            System.out.println("No se pudieron cargar las credenciales de Tienda Nube.");
            return;
        }

        List<String> ordenes = new ArrayList<>();
        ordenes.add("9504");  // envio nube
        ordenes.add("9499"); // caba llega hoy
        ordenes.add("9498"); // local del vendedor
        ordenes.add("9493"); // amba llega hoy
        ordenes.add("9490"); // zippin

        for (String o : ordenes) {
            String orderNum = o.trim();
            System.out.println("\n══════════════════════════════════════");
            System.out.println("  Buscando orden #" + orderNum);
            System.out.println("══════════════════════════════════════");

            boolean found = false;
            for (String storeName : List.of(STORE_HOGAR, STORE_GASTRO)) {
                StoreCredentials store = getStore(storeName);
                if (store == null) continue;

                String url = String.format(
                        "https://api.tiendanube.com/v1/%s/orders?q=%s&per_page=5",
                        store.storeId, orderNum);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authentication", "bearer " + store.accessToken)
                        .header("User-Agent", "Pickit")
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) continue;

                JsonNode ordersArray = mapper.readTree(response.body());
                if (!ordersArray.isArray()) continue;

                for (JsonNode order : ordersArray) {
                    long number = order.path("number").asLong(0);
                    if (!String.valueOf(number).equals(orderNum)) continue;

                    found = true;
                    System.out.println("  Store: " + storeName);

                    boolean pickup = esPickup(order);
                    String shippingName = obtenerNombreEnvio(order);
                    String nota = order.path("owner_note").asString("").trim();
                    String shippingStatus = order.path("shipping_status").asString("");

                    // Clasificación
                    String tipoEnvio;
                    String snUpper = shippingName != null ? shippingName.toUpperCase() : "";
                    if (shippingName != null && shippingName.equalsIgnoreCase("Local del Vendedor")) {
                        tipoEnvio = "RETIRO";
                    } else if (pickup && !snUpper.contains("CORREO") && !snUpper.contains("ENVÍO NUBE") && !snUpper.contains("ENVIO NUBE")) {
                        tipoEnvio = "RETIRO";
                    } else if (snUpper.contains("LLEGA HOY")) {
                        int parenIdx = shippingName.indexOf('(');
                        String clean = (parenIdx > 0 ? shippingName.substring(0, parenIdx).trim() : shippingName.trim()).toUpperCase();
                        if (clean.contains(" - ") && !clean.startsWith("LLEGA HOY")) {
                            String[] parts = clean.split("\\s*-\\s*", 2);
                            clean = parts[1] + " - " + parts[0];
                        }
                        tipoEnvio = clean;
                    } else {
                        tipoEnvio = "CORREO";
                    }

                    System.out.println("  shipping_status: " + shippingStatus);
                    System.out.println("  shippingName   : " + shippingName);
                    System.out.println("  esPickup       : " + pickup);
                    System.out.println("  nota           : " + (nota.isEmpty() ? "(vacía)" : nota));
                    System.out.println("  → tipoEnvio    : " + tipoEnvio);

                    // Fulfillments completos
                    JsonNode fulfillments = order.path("fulfillments");
                    if (fulfillments.isArray()) {
                        for (int i = 0; i < fulfillments.size(); i++) {
                            JsonNode fo = fulfillments.get(i);
                            JsonNode shipping = fo.path("shipping");
                            System.out.println("  fulfillment[" + i + "]:");
                            System.out.println("    status       : " + fo.path("status").asString(""));
                            System.out.println("    shipping.type: " + shipping.path("type").asString(""));
                            System.out.println("    carrier.name : " + shipping.path("carrier").path("name").asString(""));
                            System.out.println("    option.name  : " + shipping.path("option").path("name").asString(""));
                            System.out.println("    option.code  : " + shipping.path("option").path("code").asString(""));
                        }
                    }

                    // shipping_option a nivel de orden (fallback)
                    JsonNode shippingOption = order.path("shipping_option");
                    if (!shippingOption.isMissingNode() && !shippingOption.isNull()) {
                        System.out.println("  shipping_option (orden):");
                        if (shippingOption.isString()) {
                            System.out.println("    value: " + shippingOption.asString(""));
                        } else {
                            System.out.println("    name : " + shippingOption.path("name").asString(""));
                            System.out.println("    code : " + shippingOption.path("code").asString(""));
                        }
                    }

                    // Productos
                    JsonNode products = order.path("products");
                    if (products.isArray()) {
                        System.out.println("  productos:");
                        for (JsonNode p : products) {
                            System.out.printf("    - %s (SKU: %s) x%.0f%n",
                                    p.path("name").asString(""), p.path("sku").asString(""), p.path("quantity").asDouble(0));
                        }
                    }
                }
            }

            if (!found) {
                System.out.println("  Orden no encontrada en ninguna tienda.");
            }
        }
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
