# Ordenador de Etiquetas ZPL

Aplicación de escritorio JavaFX para gestionar, ordenar e imprimir etiquetas de envío en formato ZPL (Zebra Programming Language), con integración a la API de MercadoLibre.

## Funcionalidades

- **Archivo local:** Cargar archivos `.txt` con etiquetas ZPL, parsearlas y ordenarlas por zona.
- **API MercadoLibre:** Obtener órdenes pendientes de envío (ME2), descargar etiquetas ZPL e inyectar información de zona y código externo.
- **Ordenamiento por zona:** Asigna zonas (J1, J2, T1, T2, CARROS, RETIROS, etc.) a cada etiqueta según un mapeo SKU-Zona definido en un archivo Excel.
- **Composición de combos:** Lee un Excel con productos compuestos y muestra/imprime su desglose.
- **Impresión directa:** Envía las etiquetas ZPL ordenadas directamente a una impresora Zebra.
- **Guardado de archivo:** Exporta las etiquetas ordenadas a un archivo `.txt`.

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

## Estructura del proyecto

```
src/main/java/ar/com/leo/
├── api/ml/          # Integración con API de MercadoLibre
├── model/           # Records: ZplLabel, ExcelMapping, ComboProduct, etc.
├── parser/          # Parseo de archivos ZPL y Excel
├── printer/         # Descubrimiento de impresoras y envío ZPL
├── sorter/          # Ordenamiento de etiquetas por zona
├── ui/              # Controladores JavaFX y diálogos
├── util/            # Utilidades (decodificación hex ZPL)
├── AppLogger.java
└── EtiquetasApp.java
```

## Tecnologías

- **JavaFX 25** + AtlantaFX (tema PrimerLight)
- **Apache POI** - Lectura de archivos Excel
- **Jackson** - Procesamiento JSON (API ML)
- **Guava** - RateLimiter para llamadas a la API
- **Log4j 2** - Logging
