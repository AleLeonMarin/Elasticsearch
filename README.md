# 📊 Java Elasticsearch Dashboard

Un proyecto completo que demuestra la integración entre **Java**, **Elasticsearch** y **JavaFX** para crear un sistema de indexación y visualización de datos desde archivos Excel.

## 🎯 Descripción del Proyecto

Este proyecto implementa un pipeline completo de **ETL (Extract, Transform, Load)** que:

1. **Extrae** datos de archivos Excel (.xlsx)
2. **Transforma** los datos en documentos JSON
3. **Carga** los datos en Elasticsearch para indexación
4. **Visualiza** los datos a través de una interfaz JavaFX con tablas y gráficos interactivos

## 🏗️ Arquitectura del Sistema

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Excel Files   │───▶│  Apache POI     │───▶│  Elasticsearch  │───▶│   JavaFX UI     │
│    (.xlsx)      │    │  (ExcelReader)  │    │   (Indexing)    │    │ (Visualization) │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🚀 Tecnologías Utilizadas

### Core Technologies
- **Java 21**: Lenguaje de programación principal
- **JavaFX 21.0.1**: Framework para la interfaz gráfica
- **Maven**: Gestión de dependencias y construcción del proyecto

### Elasticsearch Stack
- **Elasticsearch 8.11.0**: Motor de búsqueda y análisis
- **Elasticsearch Java Client**: Cliente oficial para Java
- **Docker**: Contenedorización de Elasticsearch

### Procesamiento de Datos
- **Apache POI 5.3.0**: Lectura de archivos Excel
- **Jackson 2.15.2**: Serialización/deserialización JSON

## 🐳 Configuración de Elasticsearch con Docker

### Imagen de Docker Utilizada

```bash
# Descargar la imagen oficial de Elasticsearch desde Docker Hub
docker pull docker.elastic.co/elasticsearch/elasticsearch:8.19.4

# Ejecutar Elasticsearch en modo de desarrollo
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.19.4
```

### Verificación de la Conexión

```bash
# Verificar que Elasticsearch está ejecutándose
curl -X GET "localhost:9200/"
```

Respuesta esperada:
```json
{
  "name" : "docker-cluster",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "...",
  "version" : {
    "number" : "8.19.4",
    "lucene_version" : "9.12.2"
  }
}
```

## 🔧 Conexión a Elasticsearch

### Código de Conexión

```java
public class ElasticConnection {
    private ElasticsearchClient client;
    private ElasticsearchTransport transport;
    private RestClient restClient;

    public ElasticsearchClient connect() throws IOException {
        if (client == null) {
            // Crear el cliente REST de bajo nivel
            restClient = RestClient.builder(
                    new HttpHost("localhost", 9200, "http")
            ).build();

            // Crear el transporte con el mapper JSON
            transport = new RestClientTransport(
                    restClient, new JacksonJsonpMapper()
            );

            // Crear el cliente de Elasticsearch
            client = new ElasticsearchClient(transport);
        }
        return client;
    }
}
```

### Características de la Conexión

- **Host**: `localhost:9200` (configuración por defecto)
- **Protocolo**: HTTP (sin SSL para desarrollo)
- **Transport**: RestClientTransport con Jackson JSON mapper
- **Pool de Conexiones**: Manejo automático por el RestClient

## 📋 Requisitos

1. **Java 21 o superior** (necesario para JavaFX 21 en ARM64)
2. **Elasticsearch ejecutándose localmente:**
   ```bash
   # Usando Docker (recomendado)
   docker run -d --name elasticsearch \
     -p 9200:9200 \
     -e "discovery.type=single-node" \
     -e "xpack.security.enabled=false" \
     docker.elastic.co/elasticsearch/elasticsearch:8.11.0
   ```

## 🚀 Uso

### Compilar y ejecutar:
```bash
mvn clean compile
mvn javafx:run
```

### Prueba rápida sin interfaz gráfica:
```bash
mvn exec:java -Dexec.mainClass="com.una.ale.util.ElasticsearchTest"
```

### Uso programático:
```java
ElasticsearchService service = new ElasticsearchService();
try {
    boolean connected = service.testConnection();
    if (connected) {
        String info = service.getClusterInfo();
        System.out.println(info);
    }
} finally {
    service.close();
}
```


## 🔧 Características

- 🔗 Conexión automática a Elasticsearch
- 🖥️ Interfaz gráfica JavaFX intuitiva
- 📊 Información del cluster en tiempo real
- 🔒 Manejo seguro de recursos y conexiones
- 🧪 Clase de prueba independiente
- 📱 Compatible con Mac ARM64 (M1/M2/M3)

## ⚠️ Notas Importantes

- El proyecto usa JavaFX 21 para compatibilidad ARM64
- La conexión se configura para localhost:9200 sin autenticación
- Recuerda siempre cerrar las conexiones para liberar recursos
- Para producción, considera configurar autenticación y SSL


## 🚀 Instalación y Ejecución

### Prerrequisitos
```bash
# Java 21
java --version

# Maven
mvn --version

# Docker (para Elasticsearch)
docker --version
```

### Pasos de Instalación

1. **Clonar el repositorio**:
```bash
git clone <repository-url>
cd elastic
```

2. **Iniciar Elasticsearch**:
```bash
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.19.4
```

3. **Compilar el proyecto**:
```bash
mvn clean compile
```

4. **Ejecutar la aplicación**:
```bash
mvn javafx:run
```

### Verificación de Funcionamiento

1. **Conexión a Elasticsearch**: El status debe mostrar "Conectado a Elasticsearch"
2. **Carga de Datos**: La tabla debe poblarse con datos del Excel
3. **Visualización**: El gráfico debe mostrar barras con datos agrupados

## 📁 Estructura Completa del Proyecto

```
src/
├── main/
│   ├── java/
│   │   └── com/una/ale/
│   │       ├── App.java                    # Clase principal
│   │       ├── controllers/
│   │       │   └── MainController.java     # Controlador JavaFX
│   │       ├── models/                     # Modelos de datos
│   │       ├── services/
│   │       │   └── ElasticsearchService.java # Servicios de ES
│   │       └── util/
│   │           ├── ElasticConnection.java   # Conexión a ES
│   │           └── ExcelReader.java        # Lector de Excel
│   └── resources/
│       └── com/una/ale/
│           ├── resources/
│           │   └── excel/
│           │       └── ventas.xlsx         # Datos de ejemplo
│           └── view/
│               └── main.fxml               # Interfaz JavaFX
```

## ⚙️ Configuración

### Elasticsearch
- **Host**: localhost
- **Puerto**: 9200
- **Índice por defecto**: `excel_ventas`

### JavaFX
- **Resolución**: 1200x800 pixels
- **Tema**: Diseño moderno con CSS integrado

### Archivos Excel
- **Ubicación**: `src/main/resources/com/una/ale/resources/excel/`
- **Formato**: .xlsx (Excel 2007+)
- **Estructura esperada**: id, fecha, cliente, producto, cantidad, precio_unitario, total, provincia

## 📄 Proceso de Indexación

### 1. Lectura del Excel

```java
public class ExcelReader {
    public Map<Integer, List<String>> readExcel(String filePath) throws IOException {
        Map<Integer, List<String>> data = new HashMap<>();
        
        try (FileInputStream file = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(file)) {
            
            XSSFSheet sheet = workbook.getSheetAt(0);
            
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                List<String> rowData = new ArrayList<>();
                
                if (row != null) {
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        Cell cell = row.getCell(j);
                        rowData.add(getCellValueAsString(cell));
                    }
                }
                data.put(i, rowData);
            }
        }
        return data;
    }
}
```

### 2. Transformación de Datos

Los datos del Excel se transforman en documentos JSON:

```java
private Map<String, Object> createDocument(List<String> headers, List<String> row) {
    Map<String, Object> document = new HashMap<>();
    
    for (int i = 0; i < headers.size() && i < row.size(); i++) {
        String header = headers.get(i);
        String value = row.get(i);
        
        // Conversión de tipos específicos
        if ("total".equals(header) || "precio_unitario".equals(header)) {
            document.put(header, parseDouble(value));
        } else if ("cantidad".equals(header)) {
            document.put(header, parseInt(value));
        } else {
            document.put(header, value);
        }
    }
    
    return document;
}
```

### 3. Indexación Masiva (Bulk API)

```java
public int bulkIndexDocuments(String indexName, List<Map<String, Object>> documents) throws IOException {
    BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
    
    for (Map<String, Object> document : documents) {
        bulkBuilder.operations(op -> op
            .index(idx -> idx
                .index(indexName)
                .document(document)
            )
        );
    }
    
    BulkResponse bulkResponse = client.bulk(bulkBuilder.build());
    
    if (bulkResponse.errors()) {
        // Manejo de errores...
    }
    
    return documents.size() - (int) bulkResponse.items().stream()
        .filter(item -> item.error() != null)
        .count();
}
```

## 🔍 Proceso de Indexación en Elasticsearch

### ¿Qué hace Elasticsearch al indexar?

1. **Análisis del Documento**:
   - Elasticsearch analiza cada campo del documento JSON
   - Determina automáticamente el tipo de datos (string, number, date, etc.)
   - Aplica analizadores de texto para campos de tipo texto

2. **Creación del Mapping**:
   ```json
   {
     "mappings": {
       "properties": {
         "id": { "type": "long" },
         "fecha": { "type": "text" },
         "cliente": { "type": "text" },
         "producto": { "type": "text" },
         "cantidad": { "type": "long" },
         "precio_unitario": { "type": "double" },
         "total": { "type": "double" },
         "provincia": { "type": "text" }
       }
     }
   }
   ```

3. **Almacenamiento y Sharding**:
   - Los documentos se distribuyen en shards (fragmentos)
   - Cada shard se replica para alta disponibilidad
   - Se crean índices invertidos para búsquedas rápidas

4. **Indexación de Términos**:
   - Para campos de texto: tokenización, normalización, stemming
   - Para campos numéricos: indexación de rangos para consultas eficientes
   - Para fechas: conversión a timestamp interno

### Estructura del Índice Creado

```bash
# Verificar el mapping del índice
curl -X GET "localhost:9200/excel_ventas/_mapping"

# Ver estadísticas del índice
curl -X GET "localhost:9200/excel_ventas/_stats"

# Contar documentos
curl -X GET "localhost:9200/excel_ventas/_count"
```

## 🎮 Funcionalidades de la Aplicación

### Panel Principal
- **Tabla de Datos**: Muestra todos los registros indexados
- **Gráfico de Barras**: Visualización interactiva con múltiples agrupaciones
- **Controles de Gráfico**: ComboBox para seleccionar tipo de visualización

### Tipos de Análisis Disponibles
1. **Ventas por Producto**: Top productos más vendidos
2. **Ventas por Provincia**: Distribución geográfica de ventas
3. **Ventas por Cliente**: Clientes más importantes
4. **Ventas por Mes**: Tendencias temporales
5. **Cantidad por Producto**: Volumen de productos vendidos

### Operaciones Disponibles
- **🔄 Reindexar Datos**: Actualizar índice con nuevos datos
- **🔗 Reconectar ES**: Reestablecer conexión con Elasticsearch
- **📊 Cargar Datos**: Refrescar tabla y gráficos
- **📋 Ver en Consola**: Mostrar datos en terminal
- **🖥️ Visualizador Gráfico**: Ventana dedicada de visualización

## 📊 Visualización de Datos con Java

### Interfaz JavaFX vs Kibana

| Característica | JavaFX (Este Proyecto) | Kibana |
|---|---|---|
| **Integración** | Nativa con aplicación Java | Herramienta externa |
| **Personalización** | Control total sobre UI/UX | Templates predefinidos |
| **Complejidad** | Requiere desarrollo | Configuración visual |
| **Flexibilidad** | Alta - código personalizado | Media - widgets limitados |

### Cuándo Usar Cada Uno

**Usar Java + JavaFX cuando**:
- Necesitas integración con aplicación existente
- Requieres lógica de negocio específica
- Tienes requisitos de UI muy particulares
- El equipo es principalmente de desarrollo Java

**Usar Kibana cuando**:
- Necesitas dashboards rápidos para análisis
- Trabajas con grandes volúmenes de datos
- Requieres funcionalidades avanzadas de análisis
- El equipo incluye analistas de datos


### Componentes de Visualización

#### 1. Tabla de Datos
```java
@FXML
private void setupTableColumns() {
    // Configuración dinámica de columnas
    colId.setCellValueFactory(data -> 
        new SimpleStringProperty(data.getValue().get("id").toString()));
    
    colFecha.setCellValueFactory(data -> 
        new SimpleStringProperty(data.getValue().get("fecha").toString()));
    
    // ... más columnas
}
```

#### 2. Gráficos Interactivos
```java
public enum ChartType {
    PRODUCTO("Producto", "producto", "Ventas por Producto"),
    PROVINCIA("Provincia", "provincia", "Ventas por Provincia"), 
    CLIENTE("Cliente", "cliente", "Ventas por Cliente"),
    MES("Mes", "fecha", "Ventas por Mes");
    
    // Configuración dinámica de agrupaciones
}

private void updateChart(List<Map<String, Object>> documents, ChartType chartType) {
    Map<String, Double> groupedData = documents.stream()
        .filter(doc -> doc.get(chartType.getFieldName()) != null)
        .collect(Collectors.groupingBy(
            doc -> doc.get(chartType.getFieldName()).toString(),
            Collectors.summingDouble(doc -> parseDouble(doc.get("total")))
        ));
    
    // Crear series de datos para BarChart
    XYChart.Series<String, Number> series = new XYChart.Series<>();
    series.setName(chartType.getDisplayName());
    
    groupedData.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
        .limit(10)
        .forEach(entry -> series.getData().add(
            new XYChart.Data<>(entry.getKey(), entry.getValue())
        ));
}
```

#### 3. Operaciones Asíncronas
```java
// Evitar bloqueo de la interfaz
CompletableFuture.runAsync(() -> {
    try {
        List<Map<String, Object>> documents = elasticsearchService.searchDocuments(indexName);
        
        Platform.runLater(() -> {
            updateTable(documents);
            updateChart(documents);
        });
    } catch (Exception e) {
        Platform.runLater(() -> showError(e.getMessage()));
    }
});
```


## 📈 Métricas del Proyecto

### Datos de Ejemplo
- **50 registros** de ventas indexados
- **5 productos** diferentes
- **7 provincias** cubiertas
- **Período**: Datos de ejemplo de ventas mensuales

### Performance
- **Indexación**: ~50 documentos en <1 segundo
- **Consultas**: Respuesta instantánea para datasets pequeños
- **UI**: Actualización en tiempo real sin bloqueos

## 🤝 Contribución

Este proyecto está diseñado como material educativo para demostrar:
- Integración Java-Elasticsearch
- Desarrollo de interfaces con JavaFX
- Patrones ETL con tecnologías modernas
- Visualización de datos programática vs herramientas especializadas

## 📄 Licencia

Proyecto educativo - Universidad Nacional de Costa Rica

---

**Desarrollado por Alejandro León Marín usando Java, Elasticsearch, Docker y JavaFX**