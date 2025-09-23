package com.una.ale.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.una.ale.services.ElasticsearchService;
import com.una.ale.util.ExcelReader;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Controlador principal de la aplicación JavaFX
 * Maneja la conexión a Elasticsearch y la indexación de datos Excel
 */
public class MainController {

    /**
     * Enum para los diferentes tipos de gráficos disponibles
     */
    public enum ChartType {
        PRODUCTO("Producto", "producto", "Ventas por Producto"),
        PROVINCIA("Provincia", "provincia", "Ventas por Provincia"), 
        CLIENTE("Cliente", "cliente", "Ventas por Cliente"),
        MES("Mes", "fecha", "Ventas por Mes"),
        CANTIDAD_PRODUCTO("Cantidad por Producto", "producto", "Cantidad Vendida por Producto");
        
        private final String displayName;
        private final String fieldName;
        private final String chartTitle;
        
        ChartType(String displayName, String fieldName, String chartTitle) {
            this.displayName = displayName;
            this.fieldName = fieldName;
            this.chartTitle = chartTitle;
        }
        
        public String getDisplayName() { return displayName; }
        public String getFieldName() { return fieldName; }
        public String getChartTitle() { return chartTitle; }
        
        @Override
        public String toString() { return displayName; }
    }

    // Constantes de configuración
    private static final String EXCEL_FILE_PATH = "src/main/resources/com/una/ale/resources/excel/ventas.xlsx";
    private static final String DEFAULT_INDEX_NAME = "excel_ventas";
    
    // Componentes FXML
    @FXML
    private Label txtStatus;
    @FXML
    private TableView<Map<String, Object>> tblData;
    @FXML
    private TableColumn<Map<String, Object>, String> colId;
    @FXML
    private TableColumn<Map<String, Object>, String> colFecha;
    @FXML
    private TableColumn<Map<String, Object>, String> colCliente;
    @FXML
    private TableColumn<Map<String, Object>, String> colProducto;
    @FXML
    private TableColumn<Map<String, Object>, String> colCantidad;
    @FXML
    private TableColumn<Map<String, Object>, String> colTotal;
    @FXML
    private TableColumn<Map<String, Object>, String> colProvincia;
    @FXML
    private BarChart<String, Number> barChart;
    @FXML
    private CategoryAxis xAxis;
    @FXML
    private NumberAxis yAxis;
    @FXML
    private ComboBox<ChartType> cmbChartType;
    @FXML
    private Button btnUpdateChart;
    
    // Componentes de búsqueda
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbSearchField;
    @FXML
    private Button btnSearch;
    @FXML
    private Button btnClearSearch;
    @FXML
    private Label lblSearchResults;

    // Servicios
    private final ElasticsearchService elasticsearchService;
    private final ExcelReader excelReader;
    
    // Datos
    private ObservableList<Map<String, Object>> tableData;
    private ObservableList<Map<String, Object>> originalData; // Para restaurar después de búsquedas
    
    // Estado de la aplicación
    private boolean isElasticsearchConnected = false;
    private int lastIndexedCount = 0;
    private boolean isSearchActive = false;

    /**
     * Constructor - inicializa los servicios
     */
    public MainController() {
        this.elasticsearchService = new ElasticsearchService();
        this.excelReader = new ExcelReader();
        this.tableData = FXCollections.observableArrayList();
        this.originalData = FXCollections.observableArrayList();
    }

    /**
     * Método de inicialización de JavaFX
     * Se ejecuta automáticamente después de cargar el FXML
     */
    @FXML
    private void initialize() {
        setupTableColumns();
        setupChart();
        setupChartTypeComboBox();
        setupSearchComponents();
        updateStatus("🔄 Inicializando...", false);
        
        // Ejecutar inicialización en background para no bloquear UI
        CompletableFuture.runAsync(() -> {
            initializeElasticsearch();
            processExcelData();
        }).exceptionally(throwable -> {
            Platform.runLater(() -> 
                updateStatus("❌ Error de inicialización: " + throwable.getMessage(), true)
            );
            return null;
        });
    }
    
    /**
     * Configura el ComboBox con los tipos de gráfico disponibles
     */
    private void setupChartTypeComboBox() {
        cmbChartType.setItems(FXCollections.observableArrayList(ChartType.values()));
        cmbChartType.setValue(ChartType.PRODUCTO); // Valor por defecto
    }
    
    /**
     * Configura los componentes de búsqueda
     */
    private void setupSearchComponents() {
        // Configurar ComboBox de campos de búsqueda
        cmbSearchField.setItems(FXCollections.observableArrayList(
            "Todos los campos",
            "cliente", 
            "producto", 
            "provincia", 
            "fecha"
        ));
        cmbSearchField.setValue("Todos los campos");
        
        // Configurar placeholder y estilo del campo de búsqueda
        txtSearch.setPromptText("Escribe aquí para buscar...");
        
        // Inicializar label de resultados
        lblSearchResults.setText("");
    }

    /**
     * Configura las columnas de la tabla
     */
    private void setupTableColumns() {
        if (tblData != null) {
            // Configurar cell value factories
            colId.setCellValueFactory(cellData -> 
                new SimpleStringProperty(getFieldValue(cellData.getValue(), "id")));
            colFecha.setCellValueFactory(cellData -> 
                new SimpleStringProperty(getFieldValue(cellData.getValue(), "fecha")));
            colCliente.setCellValueFactory(cellData -> 
                new SimpleStringProperty(getFieldValue(cellData.getValue(), "cliente")));
            colProducto.setCellValueFactory(cellData -> 
                new SimpleStringProperty(getFieldValue(cellData.getValue(), "producto")));
            colCantidad.setCellValueFactory(cellData -> 
                new SimpleStringProperty(getFieldValue(cellData.getValue(), "cantidad")));
            colTotal.setCellValueFactory(cellData -> 
                new SimpleStringProperty(getFieldValue(cellData.getValue(), "total")));
            colProvincia.setCellValueFactory(cellData -> 
                new SimpleStringProperty(getFieldValue(cellData.getValue(), "provincia")));

            // Asociar datos a la tabla
            tblData.setItems(tableData);
        }
    }

    /**
     * Configura el gráfico de barras
     */
    private void setupChart() {
        if (barChart != null) {
            barChart.setTitle("Ventas por Producto");
            xAxis.setLabel("Productos");
            yAxis.setLabel("Total de Ventas");
            barChart.setLegendVisible(false);
        }
    }

    /**
     * Obtiene el valor de un campo del documento
     */
    private String getFieldValue(Map<String, Object> document, String fieldName) {
        Object value = document.get(fieldName);
        if (value == null) return "-";
        
        // Formatear valores específicos
        if (fieldName.equals("total") || fieldName.equals("precio_unitario")) {
            try {
                double numValue = Double.parseDouble(value.toString());
                return String.format("₡%.0f", numValue);
            } catch (NumberFormatException e) {
                return value.toString();
            }
        }
        
        return value.toString();
    }

    /**
     * Inicializa y prueba la conexión a Elasticsearch
     */
    private void initializeElasticsearch() {
        try {
            logInfo("🔍 Verificando conexión a Elasticsearch...");
            
            boolean connected = elasticsearchService.testConnection();
            this.isElasticsearchConnected = connected;
            
            if (connected) {
                String clusterInfo = elasticsearchService.getClusterInfo();
                logInfo("✅ " + clusterInfo);
                Platform.runLater(() -> 
                    updateStatus("✅ Conectado a Elasticsearch", false)
                );
            } else {
                logError("❌ No se pudo conectar a Elasticsearch");
                Platform.runLater(() -> 
                    updateStatus("❌ Sin conexión a Elasticsearch", true)
                );
            }
            
        } catch (Exception e) {
            this.isElasticsearchConnected = false;
            logError("❌ Error conectando a Elasticsearch: " + e.getMessage());
            Platform.runLater(() -> 
                updateStatus("❌ Error: " + e.getMessage(), true)
            );
        }
    }

    /**
     * Procesa y opcionalmente indexa datos del archivo Excel
     */
    private void processExcelData() {
        if (!isElasticsearchConnected) {
            logWarning("⚠️ Saltando indexación - sin conexión a Elasticsearch");
            return;
        }

        try {
            // Verificar si ya hay datos en el índice
            long existingCount = elasticsearchService.countDocuments(DEFAULT_INDEX_NAME);
            if (existingCount > 0) {
                logInfo("📊 Ya hay " + existingCount + " documentos en el índice '" + DEFAULT_INDEX_NAME + "'");
                logInfo("⏭️ Saltando indexación - datos ya existen");
                
                Platform.runLater(() -> {
                    updateStatus("✅ " + existingCount + " docs ya indexados", false);
                    loadDataToTable();
                });
                return;
            }
            
            logInfo("📊 Procesando archivo Excel: " + EXCEL_FILE_PATH);
            
            // Leer datos del Excel
            Map<Integer, List<String>> excelData = readExcelData();
            if (excelData == null || excelData.isEmpty()) {
                return; // Error ya manejado en readExcelData()
            }
            
            // Indexar datos
            indexDataToElasticsearch(excelData);
            
        } catch (Exception e) {
            logError("❌ Error procesando Excel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lee los datos del archivo Excel
     * @return Map con los datos o null si hay error
     */
    private Map<Integer, List<String>> readExcelData() {
        try {
            Map<Integer, List<String>> excelData = excelReader.readExcel(EXCEL_FILE_PATH);
            
            if (excelData.isEmpty()) {
                logWarning("⚠️ No se encontraron datos en el archivo Excel");
                return null;
            }
            
            List<String> headers = excelData.get(0);
            int dataRows = excelData.size() - 1;
            
            logInfo("📋 Headers encontrados: " + headers);
            logInfo("📊 Filas de datos: " + dataRows);
            
            return excelData;
            
        } catch (IOException e) {
            logError("❌ Error leyendo Excel: " + e.getMessage());
            logInfo("💡 Verifica que el archivo existe en: " + EXCEL_FILE_PATH);
            logInfo("💡 O ejecuta ExcelCreator para crear un archivo de ejemplo");
            return null;
        }
    }

    /**
     * Indexa los datos del Excel en Elasticsearch
     * @param excelData Datos del Excel a indexar
     */
    private void indexDataToElasticsearch(Map<Integer, List<String>> excelData) {
        try {
            List<String> headers = excelData.get(0);
            int indexedCount = elasticsearchService.indexExcelData(DEFAULT_INDEX_NAME, excelData, headers);
            
            this.lastIndexedCount = indexedCount;
            
            if (indexedCount > 0) {
                String successMessage = String.format(
                    "✅ Indexación exitosa: %d documentos en '%s'", 
                    indexedCount, DEFAULT_INDEX_NAME
                );
                logInfo(successMessage);
                
                Platform.runLater(() -> {
                    updateStatus("✅ " + indexedCount + " docs indexados", false);
                    // Cargar datos en tabla y gráfico
                    loadDataToTable();
                });
            } else {
                logWarning("⚠️ No se pudieron indexar los datos");
                Platform.runLater(() -> 
                    updateStatus("⚠️ Indexación falló", true)
                );
            }
            
        } catch (Exception e) {
            logError("❌ Error indexando datos: " + e.getMessage());
            Platform.runLater(() -> 
                updateStatus("❌ Error de indexación", true)
            );
        }
    }

    /**
     * Carga los datos de Elasticsearch a la tabla y actualiza el gráfico
     */
    private void loadDataToTable() {
        CompletableFuture.runAsync(() -> {
            try {
                if (!isElasticsearchConnected) {
                    logWarning("⚠️ No hay conexión a Elasticsearch");
                    return;
                }

                logInfo("📊 Cargando datos de " + DEFAULT_INDEX_NAME + " para mostrar en tabla...");
                
                // Obtener datos de Elasticsearch
                List<Map<String, Object>> documents = elasticsearchService.searchDocuments(DEFAULT_INDEX_NAME, 50);
                
                Platform.runLater(() -> {
                    if (documents != null && !documents.isEmpty()) {
                        tableData.clear();
                        tableData.addAll(documents);
                        
                        // Actualizar datos originales si no estamos en búsqueda
                        if (!isSearchActive) {
                            originalData.clear();
                            originalData.addAll(documents);
                        }
                        
                        logInfo("✅ " + documents.size() + " registros cargados en la tabla");
                        
                        // Actualizar gráfico
                        updateChart(documents);
                    } else {
                        tableData.clear();
                        logWarning("⚠️ No se encontraron datos para mostrar");
                        // Limpiar gráfico cuando no hay datos
                        updateChart(new ArrayList<>());
                    }
                });
                
            } catch (Exception e) {
                logError("❌ Error cargando datos para tabla: " + e.getMessage());
                Platform.runLater(() -> 
                    updateStatus("❌ Error cargando datos", true)
                );
            }
        });
    }

    /**
     * Actualiza el gráfico de barras con los datos según el tipo seleccionado
     */
    private void updateChart(List<Map<String, Object>> documents) {
        ChartType selectedType = cmbChartType.getValue();
        if (selectedType == null) {
            selectedType = ChartType.PRODUCTO;
        }
        updateChart(documents, selectedType);
    }
    
    /**
     * Actualiza el gráfico de barras con los datos según el tipo especificado
     */
    private void updateChart(List<Map<String, Object>> documents, ChartType chartType) {
        try {
            if (barChart == null) return;
            
            // Configurar el título del gráfico
            barChart.setTitle(chartType.getChartTitle());
            
            // Limpiar gráfico si no hay datos
            if (documents == null || documents.isEmpty()) {
                barChart.getData().clear();
                logInfo("📈 Gráfico limpiado - sin datos para mostrar");
                return;
            }
            
            Map<String, Double> groupedData;
            
            switch (chartType) {
                case PRODUCTO:
                    groupedData = documents.stream()
                        .filter(doc -> doc.get("producto") != null && doc.get("total") != null)
                        .collect(Collectors.groupingBy(
                            doc -> doc.get("producto").toString(),
                            Collectors.summingDouble(doc -> parseDouble(doc.get("total")))
                        ));
                    break;
                    
                case PROVINCIA:
                    groupedData = documents.stream()
                        .filter(doc -> doc.get("provincia") != null && doc.get("total") != null)
                        .collect(Collectors.groupingBy(
                            doc -> doc.get("provincia").toString(),
                            Collectors.summingDouble(doc -> parseDouble(doc.get("total")))
                        ));
                    break;
                    
                case CLIENTE:
                    groupedData = documents.stream()
                        .filter(doc -> doc.get("cliente") != null && doc.get("total") != null)
                        .collect(Collectors.groupingBy(
                            doc -> doc.get("cliente").toString(),
                            Collectors.summingDouble(doc -> parseDouble(doc.get("total")))
                        ));
                    break;
                    
                case MES:
                    groupedData = documents.stream()
                        .filter(doc -> doc.get("fecha") != null && doc.get("total") != null)
                        .collect(Collectors.groupingBy(
                            doc -> extractMonth(doc.get("fecha").toString()),
                            Collectors.summingDouble(doc -> parseDouble(doc.get("total")))
                        ));
                    break;
                    
                case CANTIDAD_PRODUCTO:
                    groupedData = documents.stream()
                        .filter(doc -> doc.get("producto") != null && doc.get("cantidad") != null)
                        .collect(Collectors.groupingBy(
                            doc -> doc.get("producto").toString(),
                            Collectors.summingDouble(doc -> parseDouble(doc.get("cantidad")))
                        ));
                    break;
                    
                default:
                    groupedData = new HashMap<>();
            }

            // Crear serie de datos para el gráfico
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(chartType.getDisplayName());

            // Agregar datos al gráfico (limitar a los top 10 para mejor visualización)
            groupedData.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));

            // Limpiar y agregar nueva serie
            barChart.getData().clear();
            barChart.getData().add(series);
            
            logInfo("📈 Gráfico actualizado (" + chartType.getDisplayName() + ") con " + groupedData.size() + " elementos");
            
        } catch (Exception e) {
            logError("❌ Error actualizando gráfico: " + e.getMessage());
        }
    }
    
    /**
     * Método auxiliar para parsear valores numéricos
     */
    private double parseDouble(Object value) {
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * Método auxiliar para extraer el mes de una fecha
     */
    private String extractMonth(String dateStr) {
        try {
            // Asume formato MM/dd/yyyy o similar
            String[] parts = dateStr.split("/");
            if (parts.length >= 2) {
                int month = Integer.parseInt(parts[0]);
                String[] months = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                                   "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
                return months[month - 1];
            }
        } catch (Exception e) {
            // Si no se puede parsear, devolver el string original
        }
        return dateStr;
    }
    
    /**
     * Maneja el evento del botón para actualizar el gráfico
     */
    @FXML
    private void onUpdateChart() {
        ChartType selectedType = cmbChartType.getValue();
        if (selectedType == null) {
            selectedType = ChartType.PRODUCTO;
        }
        
        // Obtener datos actuales de la tabla y actualizar gráfico
        List<Map<String, Object>> currentData = new ArrayList<>(tableData);
        
        // Solo actualizar si hay datos para mostrar
        if (!currentData.isEmpty()) {
            updateChart(currentData, selectedType);
            logInfo("🔄 Gráfico actualizado a: " + selectedType.getDisplayName());
        } else {
            updateChart(new ArrayList<>(), selectedType);
            logInfo("🔄 Gráfico limpiado - sin datos actuales");
        }
    }

    /**
     * Método público para reindexar datos (llamado desde botones FXML)
     */
    @FXML
    private void onReindexData() {
        updateStatus("� Reindexando...", false);
        logInfo("🔄 Iniciando reindexación manual...");
        
        CompletableFuture.runAsync(() -> {
            processExcelData();
        }).exceptionally(throwable -> {
            Platform.runLater(() -> 
                updateStatus("❌ Error en reindexación", true)
            );
            return null;
        });
    }

    /**
     * Método público para reconectar a Elasticsearch
     */
    @FXML
    private void onReconnectElasticsearch() {
        updateStatus("🔄 Reconectando...", false);
        logInfo("🔄 Iniciando reconexión manual...");
        
        CompletableFuture.runAsync(() -> {
            initializeElasticsearch();
        });
    }

    /**
     * Método público para visualizar los datos indexados
     */
    @FXML
    private void onViewIndexedData() {
        logInfo("🔍 Abriendo visualizador de datos indexados...");
        
        CompletableFuture.runAsync(() -> {
            try {
                // Mostrar información de índices en consola
                logInfo("📋 === DATOS INDEXADOS EN ELASTICSEARCH ===");
                
                if (!isElasticsearchConnected) {
                    logError("❌ No hay conexión a Elasticsearch");
                    return;
                }
                
                // Listar índices
                var indices = elasticsearchService.listAllIndices();
                logInfo("📊 Índices encontrados: " + indices.size());
                
                // Mostrar contenido del índice principal
                if (indices.contains(DEFAULT_INDEX_NAME)) {
                    logInfo("📄 Mostrando contenido de '" + DEFAULT_INDEX_NAME + "':");
                    elasticsearchService.printIndexContent(DEFAULT_INDEX_NAME, 10);
                } else {
                    logInfo("⚠️ Índice '" + DEFAULT_INDEX_NAME + "' no encontrado");
                    
                    // Mostrar otros índices disponibles
                    for (String index : indices) {
                        if (!index.startsWith(".")) { // Ignorar índices del sistema
                            long count = elasticsearchService.countDocuments(index);
                            logInfo("📋 " + index + ": " + count + " documentos");
                        }
                    }
                }
                
                Platform.runLater(() -> 
                    updateStatus("✅ Ver consola para datos indexados", false)
                );
                
            } catch (Exception e) {
                logError("❌ Error visualizando datos: " + e.getMessage());
                Platform.runLater(() -> 
                    updateStatus("❌ Error visualizando datos", true)
                );
            }
        });
    }

    /**
     * Método público para abrir la ventana de visualización gráfica
     */
    @FXML
    private void onOpenDataViewer() {
        logInfo("🖥️ Abriendo visualizador gráfico de datos...");
        
        Platform.runLater(() -> {
            try {
                // Abrir nueva ventana con el visualizador
                com.una.ale.ElasticsearchViewerApp viewerApp = new com.una.ale.ElasticsearchViewerApp();
                javafx.stage.Stage newStage = new javafx.stage.Stage();
                viewerApp.start(newStage);
                
                updateStatus("✅ Visualizador gráfico abierto", false);
                logInfo("✅ Ventana de visualización abierta exitosamente");
                
            } catch (Exception e) {
                logError("❌ Error abriendo visualizador: " + e.getMessage());
                updateStatus("❌ Error abriendo visualizador", true);
            }
        });
    }

    /**
     * Actualiza el estado en la interfaz de usuario
     * @param message Mensaje a mostrar
     * @param isError Si es un mensaje de error
     */
    private void updateStatus(String message, boolean isError) {
        if (txtStatus != null) {
            Platform.runLater(() -> {
                txtStatus.setText(message);
                // Opcional: cambiar color basado en isError
                if (isError) {
                    txtStatus.setStyle("-fx-text-fill: red;");
                } else {
                    txtStatus.setStyle("-fx-text-fill: black;");
                }
            });
        }
    }

    /**
     * Limpia recursos al cerrar la aplicación
     */
    public void cleanup() {
        try {
            if (elasticsearchService != null) {
                elasticsearchService.close();
                logInfo("🔒 Conexión a Elasticsearch cerrada");
            }
        } catch (Exception e) {
            logError("❌ Error cerrando recursos: " + e.getMessage());
        }
    }

    // Métodos utilitarios para logging consistente
    private void logInfo(String message) {
        System.out.println(message);
    }

    private void logWarning(String message) {
        System.out.println(message);
    }

    private void logError(String message) {
        System.err.println(message);
    }

    // Getters para testing o acceso externo
    public boolean isElasticsearchConnected() {
        return isElasticsearchConnected;
    }

    /**
     * Maneja el evento de búsqueda
     */
    @FXML
    private void onSearch() {
        String searchText = txtSearch.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            onClearSearch();
            return;
        }
        
        String selectedField = cmbSearchField.getValue();
        performSearch(searchText.trim(), selectedField);
    }
    
    /**
     * Maneja el evento de limpiar búsqueda
     */
    @FXML
    private void onClearSearch() {
        txtSearch.clear();
        cmbSearchField.setValue("Todos los campos");
        
        // Restaurar datos originales
        if (originalData != null && !originalData.isEmpty()) {
            tableData.clear();
            tableData.addAll(originalData);
            updateChart(new ArrayList<>(originalData));
            lblSearchResults.setText("");
            isSearchActive = false;
            logInfo("🗑️ Búsqueda limpiada - mostrando todos los datos");
        } else {
            // Si no hay datos originales, limpiar todo
            tableData.clear();
            updateChart(new ArrayList<>());
            lblSearchResults.setText("");
            isSearchActive = false;
            logInfo("🗑️ Búsqueda limpiada - sin datos originales para restaurar");
        }
    }
    
    /**
     * Maneja el evento de tecla presionada en el campo de búsqueda
     */
    @FXML
    private void onSearchKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            onSearch();
        }
    }
    
    /**
     * Realiza una búsqueda en Elasticsearch
     */
    private void performSearch(String searchText, String selectedField) {
        CompletableFuture.runAsync(() -> {
            try {
                Platform.runLater(() -> {
                    lblSearchResults.setText("🔍 Buscando...");
                    btnSearch.setDisable(true);
                });
                
                List<Map<String, Object>> searchResults;
                
                if ("Todos los campos".equals(selectedField)) {
                    // Búsqueda en todos los campos
                    searchResults = elasticsearchService.searchAllFields(DEFAULT_INDEX_NAME, searchText);
                } else {
                    // Búsqueda en campo específico
                    searchResults = elasticsearchService.searchByField(DEFAULT_INDEX_NAME, selectedField, searchText);
                }
                
                Platform.runLater(() -> {
                    // Guardar datos originales si es la primera búsqueda
                    if (!isSearchActive && !tableData.isEmpty()) {
                        originalData.clear();
                        originalData.addAll(tableData);
                    }
                    
                    // Actualizar tabla con resultados
                    tableData.clear();
                    if (searchResults != null) {
                        tableData.addAll(searchResults);
                    }
                    
                    // Actualizar gráfico con los resultados actuales
                    updateChart(searchResults != null ? searchResults : new ArrayList<>());
                    
                    // Actualizar label de resultados
                    int resultCount = searchResults != null ? searchResults.size() : 0;
                    lblSearchResults.setText(String.format("📊 %d resultados encontrados", resultCount));
                    
                    isSearchActive = true;
                    btnSearch.setDisable(false);
                    
                    logInfo(String.format("🔍 Búsqueda completada: '%s' en campo '%s' - %d resultados", 
                           searchText, selectedField, resultCount));
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblSearchResults.setText("❌ Error en búsqueda");
                    btnSearch.setDisable(false);
                    logError("❌ Error en búsqueda: " + e.getMessage());
                });
            }
        });
    }

    public int getLastIndexedCount() {
        return lastIndexedCount;
    }

    /**
     * Método público para cargar datos manualmente
     */
    @FXML
    private void onLoadData() {
        if (!isElasticsearchConnected) {
            updateStatus("❌ No hay conexión a Elasticsearch", true);
            return;
        }
        
        updateStatus("🔄 Cargando datos...", false);
        loadDataToTable();
    }
}