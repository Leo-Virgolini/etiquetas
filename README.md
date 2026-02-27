# Pickit y Etiquetas

Aplicación de escritorio JavaFX para gestionar despachos de e-commerce: generación de listas de picking (pickit), armado de carros, y ordenamiento e impresión de etiquetas ZPL. Integra MercadoLibre, Tienda Nube y DUX.

## Funcionalidades

### Pickit
- **Generación de pickit:** Obtiene ventas de MercadoLibre y Tienda Nube, cruza con stock y combos, y genera un Excel con la lista de picking ordenada por sector.
- **Carros:** Genera automáticamente hojas de carros para órdenes con 3+ SKUs distintos.
- **SLA:** Filtra ventas por fecha de despacho (hasta hoy o sin límite).
- **Productos manuales:** Permite agregar productos manualmente o importarlos desde Excel.

### Etiquetas
- **Archivo local:** Cargar archivos `.txt` con etiquetas ZPL, parsearlas y ordenarlas por zona.
- **API MercadoLibre:** Obtener órdenes pendientes de envío (ME2), descargar etiquetas ZPL e inyectar información de zona y código externo.
- **Ordenamiento por zona:** Asigna zonas (J1, J2, T1, T2, CARROS, RETIROS, etc.) a cada etiqueta según un mapeo SKU-Zona definido en un archivo Excel.
- **Composición de combos:** Lee un Excel con productos compuestos y muestra/imprime su desglose.
- **Impresión directa:** Envía las etiquetas ZPL ordenadas directamente a una impresora Zebra.

> **Nota:** Al descargar etiquetas desde la API de MercadoLibre (`GET /shipment_labels`), ML cambia automáticamente el substatus del envío de `ready_to_print` a `printed`. Este es un efecto colateral del endpoint de ML, no una acción de esta aplicación.

### Pedidos
- **Generación de pedidos:** Obtiene pedidos pendientes de MercadoLibre (retiro) y Tienda Nube (HOGAR y GASTRO) en paralelo.
- **Etiquetas LLEGA HOY:** Detecta envíos con método "LLEGA HOY" (no Zippin) y extrae datos de dirección para generar etiquetas de envío.
- **Excel con 3 hojas:** Genera un archivo Excel con hojas diferenciadas: ML Pedidos Retiro, TN Pedidos, y TN Etiquetas.
- **Solo lectura:** No modifica ningún estado en las APIs de ML ni TN (solo usa endpoints GET).

## Requisitos

- Java 25+
- Maven 3.9+
- Impresora Zebra (opcional, para impresión directa)
- Credenciales de MercadoLibre (opcional, para el flujo API)

## Compilación y ejecución

```bash
mvn clean compile
mvn javafx:run
```

## Generar JAR

```bash
mvn clean package
# Genera: target/Pickit y Etiquetas.jar
```

## Estructura del proyecto

```
src/main/java/ar/com/leo/
├── api/
│   ├── ml/              # Integración con API de MercadoLibre
│   ├── nube/            # Integración con Tienda Nube
│   └── dux/             # Integración con DUX (stock)
├── pickit/
│   ├── api/             # API ML específica del pickit
│   ├── excel/           # Lectura de stock/combos y escritura del Excel pickit
│   ├── model/           # Modelos: PickitItem, CarrosOrden, Venta, etc.
│   └── service/         # PickitGenerator y PickitService
├── model/               # Records: ZplLabel, ExcelMapping, ComboProduct, etc.
├── parser/              # Parseo de archivos ZPL y Excel
├── printer/             # Descubrimiento de impresoras y envío ZPL
├── sorter/              # Ordenamiento de etiquetas por zona
├── ui/                  # Controladores JavaFX y diálogos
├── util/                # Utilidades
├── AppLogger.java
└── EtiquetasApp.java
```

## Tecnologías

- **JavaFX 25** + AtlantaFX (tema PrimerLight)
- **Apache POI** - Lectura y escritura de archivos Excel
- **Jackson 3** - Procesamiento JSON (APIs)
- **Guava** - RateLimiter para llamadas a las APIs
