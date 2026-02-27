# Pickit y Etiquetas

Aplicacion de escritorio JavaFX para gestionar despachos de e-commerce: generacion de listas de picking (pickit), armado de carros, ordenamiento e impresion de etiquetas ZPL, y generacion de pedidos/etiquetas de envio. Integra MercadoLibre, Tienda Nube y DUX ERP.

## Configuracion global

Dos selectores de archivo persistentes (guardados en `Preferences`) disponibles en todas las pestanas:

- **Excel de stock** (`Stock.xlsx`): mapea SKU a zona de almacen (J1, J2, T1, T2, etc.) y opcionalmente codigo externo. Lee desde fila 3 las columnas "Codigo Producto", "Unidad" y "Codigo Externo".
- **Excel de combos**: define productos compuestos y sus componentes. Columnas: "Codigo Compuesto", "Codigo Componente", "Cantidad".

## Funcionalidades

### Pickit

Genera un Excel de picking para el deposito con todos los pedidos pendientes de todos los canales.

- **Fuentes de datos**: MercadoLibre (ordenes `ready_to_print` y `seller_agreement`), Tienda Nube (KT HOGAR y KT GASTRO), y productos manuales.
- **Filtro SLA**: "Hasta hoy" (solo ordenes con despacho para hoy o antes) o "Sin limite" (todas las pendientes).
- **Productos manuales**: agregar SKU + cantidad manualmente o importar desde Excel.
- **Expansion de combos**: los SKU compuestos se expanden automaticamente en sus componentes con cantidades multiplicadas.
- **Consulta de stock**: busca descripcion, proveedor, sector y stock actual en DUX ERP.
- **Excel generado** (`Pickits y Carros/PICKIT_*.xlsx`) con 3 hojas:
  - **PICKIT**: lista ordenada por sector con columnas SKU, CANT, DESCRIPCION, PROVEEDOR, SECTOR, STOCK. Resaltado: coral=SKU invalido, amarillo=datos faltantes, bold=cantidad>1, naranja=stock insuficiente.
  - **CARROS**: ordenes con 3+ SKUs distintos agrupadas con letra (A, B, C...) para identificar carros fisicos.
  - **SLA**: listado de ordenes ML con fecha/hora de despacho esperado.

### Etiquetas

Dos sub-pestanas para obtener etiquetas ZPL, procesarlas y enviarlas a la impresora Zebra.

#### API MercadoLibre

1. **Obtener ordenes**: busca ordenes ME2 con estado `ready_to_ship`, con filtros por estado (pendientes/impresas/todas) y despacho (solo hoy/todas). Muestra tabla con columnas: Orden, Zona, SKU, Producto, Cantidad, Estado, Despacho.
2. **Descargar etiquetas**: descarga ZPL de las ordenes seleccionadas via API de ML (en lotes de 50). Nota: ML cambia automaticamente el substatus de `ready_to_print` a `printed` al descargar.
3. **Procesamiento automatico**: parseo ZPL, asignacion de zona, ordenamiento e inyeccion de headers.

#### Archivo Local

- Carga un archivo `.txt`/`.zpl` con etiquetas ZPL crudas y las procesa con la misma pipeline.

#### Procesamiento de etiquetas ZPL

- **Parseo**: extrae bloques `^XA...^XZ`, decodifica hex (`_XX`), extrae SKU, producto, cantidad y detalles.
- **Asignacion de zona**: cruza cada SKU contra el Excel de stock para determinar la zona de almacen.
- **Ordenamiento** por prioridad: J* > T* > COMBOS > CARROS > TURBOS > RETIROS > ??? (sin mapear).
- **Inyeccion de headers en ZPL**: agrega al codigo ZPL de cada etiqueta:
  - Numero de posicion (#1, #2...) en bold (triple render)
  - Zona ("ZONA: J5")
  - Codigo externo ("COD.EXT.: 12345")
  - Resaltado de cantidad >1 (rectangulo negro con texto inverso)
- **Interleave para impresion**: reordena las etiquetas para compensar el plegado en acordeon de la impresora termica, de modo que al cortar el stack queden en orden.
- **Impresion directa**: dialog de seleccion de zonas a imprimir + seleccion de impresora. Envia ZPL crudo via `javax.print`.
- **Combos**: muestra desglose de productos compuestos presentes en el lote para facilitar el armado.

### Pedidos

Genera un Excel con tarjetas recortables de todos los pedidos pendientes, listas para pegar en los paquetes.

- **Fuentes**: MercadoLibre (retiro, ultimos 7 dias) y Tienda Nube (KT HOGAR + KT GASTRO) en paralelo.
- **Excel generado** (`Pedidos/PEDIDOS_*.xlsx`) con hasta 3 hojas:

#### ML PEDIDOS RETIRO (violeta)
- Tarjetas recortables con borde grueso, 2 columnas por pagina.
- Cada tarjeta: N de venta (grande), fecha, nombre+usuario, tabla de productos (SKU, CANT, DETALLE).
- Productos con cantidad >1 resaltados en amarillo.
- Altura dinamica: la tarjeta crece segun la cantidad de productos.

#### TN PEDIDOS (verde)
- Mismo layout de tarjetas recortables.
- Badge de tienda (KT HOGAR / KT GASTRO) con tipo de envio (RETIRO, LLEGA HOY, etc.).

#### TN ETIQUETAS (naranja)
- Etiquetas de envio para pedidos "LLEGA HOY" (no Zippin), 10 por pagina.
- Cada etiqueta: nombre grande, domicilio, localidad, CP, telefono, observaciones.

Todas las hojas: A4 portrait, margenes estrechos, page breaks inteligentes basados en altura, numeracion de pagina.

## Integraciones

| Servicio | Uso | Credenciales |
|---|---|---|
| **MercadoLibre** | Ordenes ME2, etiquetas ZPL, SLA | `ml_credentials.json`, `ml_tokens.json` |
| **Tienda Nube** | Pedidos KT HOGAR y KT GASTRO | `nube_tokens.json` |
| **DUX ERP** | Stock, descripciones, proveedores | `dux_tokens.json` |

Credenciales almacenadas en `%PROGRAMDATA%\SuperMaster\secrets\`. Tokens ML se renuevan automaticamente al expirar.

`HttpRetryHandler` implementa: rate limiting (Guava `RateLimiter`), refresh automatico de token en 401, backoff exponencial con jitter en 429/503/5xx.

## Requisitos

- Java 25+
- Maven 3.9+
- Impresora Zebra (opcional, para impresion directa)

## Compilacion y ejecucion

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
│   ├── ml/              # Integracion con API de MercadoLibre
│   ├── nube/            # Integracion con Tienda Nube
│   └── dux/             # Integracion con DUX ERP (stock)
├── pickit/
│   ├── api/             # API ML especifica del pickit
│   ├── excel/           # Lectura de stock/combos y escritura del Excel pickit
│   ├── model/           # Modelos: PickitItem, CarrosOrden, Venta, etc.
│   └── service/         # PickitGenerator y PickitService
├── pedidos/
│   ├── api/             # API ML especifica de pedidos
│   ├── excel/           # PedidosExcelWriter (tarjetas recortables)
│   ├── model/           # PedidoML, PedidoTN, EtiquetaTN, PedidosResult
│   └── service/         # PedidosGenerator y PedidosService
├── model/               # Records: ZplLabel, ExcelMapping, ComboProduct, etc.
├── parser/              # Parseo de archivos ZPL y Excel
├── printer/             # Descubrimiento de impresoras y envio ZPL
├── sorter/              # Ordenamiento de etiquetas por zona
├── ui/                  # Controladores JavaFX y dialogos
├── util/                # Utilidades
├── AppLogger.java
└── EtiquetasApp.java
```

## Tecnologias

- **JavaFX 25** + AtlantaFX (tema PrimerLight)
- **Apache POI 5.5** - Lectura y escritura de archivos Excel
- **Jackson 3** - Procesamiento JSON (APIs)
- **Guava** - RateLimiter para llamadas a las APIs
