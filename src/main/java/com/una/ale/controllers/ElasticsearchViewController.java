package com.una.ale.controllers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.una.ale.services.ElasticsearchService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controlador para la vista de visualización de datos de Elasticsearch
 */
public class ElasticsearchViewController {

    // Componentes FXML
    @FXML
    private Label lblConnectionStatus;
    @FXML
    private ComboBox<String> cmbIndices;
    @FXML
    private Button btnRefreshIndices;
    @FXML
    private Button btnLoadData;
    @FXML
    private Button btnClearData;
    @FXML
    private Label lblIndexName;
    @FXML
    private Label lblDocumentCount;
    @FXML
    private Label lblClusterName;
    @FXML
    private Label lblElasticsearchVersion;
    @FXML
    private TableView<Map<String, Object>> tblDocuments;
    @FXML
    private TableColumn<Map<String, Object>, String> colDocumentId;
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
    private TableColumn<Map<String, Object>, String> colPrecio;
    @FXML
    private TableColumn<Map<String, Object>, String> colTotal;
    @FXML
    private TableColumn<Map<String, Object>, String> colProvincia;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblLoadedCount;

    // Servicios
    private final ElasticsearchService elasticsearchService;
    
    // Datos
    private ObservableList<Map<String, Object>> documentsData;
    
    // Estado
    private boolean isConnected = false;

    /**
     * Constructor
     */
    public ElasticsearchViewController() {
        this.elasticsearchService = new ElasticsearchService();
        this.documentsData = FXCollections.observableArrayList();
    }

    /**
     * Inicialización del controlador
     */
    @FXML
    private void initialize() {
        setupTableColumns();
        setupUI();
        initializeElasticsearchConnection();
    }

    /**
     * Configura las columnas de la tabla
     */
    private void setupTableColumns() {
        // Configurar cell value factories para cada columna
        colDocumentId.setCellValueFactory(cellData -> 
            new SimpleStringProperty(getFieldValue(cellData.getValue(), "_id")));
        
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
        
        colPrecio.setCellValueFactory(cellData -> 
            new SimpleStringProperty(getFieldValue(cellData.getValue(), "precio_unitario")));
        
        colTotal.setCellValueFactory(cellData -> 
            new SimpleStringProperty(getFieldValue(cellData.getValue(), "total")));
        
        colProvincia.setCellValueFactory(cellData -> 
            new SimpleStringProperty(getFieldValue(cellData.getValue(), "provincia")));

        // Configurar datos de la tabla
        tblDocuments.setItems(documentsData);
    }

    /**
     * Obtiene el valor de un campo del documento
     */
    private String getFieldValue(Map<String, Object> document, String fieldName) {
        Object value = document.get(fieldName);
        return value != null ? value.toString() : "-";
    }

    /**
     * Configura la interfaz de usuario inicial
     */
    private void setupUI() {
        // Configurar ComboBox
        cmbIndices.setItems(FXCollections.observableArrayList());
        
        // Configurar estado inicial de botones
        btnLoadData.setDisable(true);
        btnClearData.setDisable(true);
        
        // Agregar listener para selección de índice
        cmbIndices.valueProperty().addListener((obs, oldVal, newVal) -> {
            btnLoadData.setDisable(newVal == null);
            if (newVal != null) {
                updateIndexInfo(newVal);
            }
        });
    }

    /**
     * Inicializa la conexión a Elasticsearch
     */
    private void initializeElasticsearchConnection() {
        updateStatus("🔄 Conectando a Elasticsearch...");
        
        CompletableFuture.runAsync(() -> {
            try {
                boolean connected = elasticsearchService.testConnection();
                this.isConnected = connected;
                
                Platform.runLater(() -> {
                    if (connected) {
                        lblConnectionStatus.setText("✅ Conectado");
                        lblConnectionStatus.setStyle("-fx-text-fill: #2ecc71;");
                        updateClusterInfo();
                        refreshIndices();
                        updateStatus("✅ Conectado a Elasticsearch");
                    } else {
                        lblConnectionStatus.setText("❌ Desconectado");
                        lblConnectionStatus.setStyle("-fx-text-fill: #e74c3c;");
                        updateStatus("❌ No se pudo conectar a Elasticsearch");
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblConnectionStatus.setText("❌ Error");
                    lblConnectionStatus.setStyle("-fx-text-fill: #e74c3c;");
                    updateStatus("❌ Error: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Actualiza la información del cluster
     */
    private void updateClusterInfo() {
        CompletableFuture.runAsync(() -> {
            try {
                String clusterInfo = elasticsearchService.getClusterInfo();
                Platform.runLater(() -> {
                    if (clusterInfo != null) {
                        // Parsear información del cluster
                        String[] parts = clusterInfo.split(", ");
                        if (parts.length >= 2) {
                            lblClusterName.setText(parts[0]); // Cluster: nombre
                            lblElasticsearchVersion.setText(parts[1]); // Versión: X.X.X
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblClusterName.setText("Error obteniendo info");
                    lblElasticsearchVersion.setText("-");
                });
            }
        });
    }

    /**
     * Actualiza la información de un índice específico
     */
    private void updateIndexInfo(String indexName) {
        CompletableFuture.runAsync(() -> {
            try {
                long count = elasticsearchService.countDocuments(indexName);
                Platform.runLater(() -> {
                    lblIndexName.setText("Índice: " + indexName);
                    lblDocumentCount.setText("Documentos: " + count);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblDocumentCount.setText("Error contando documentos");
                });
            }
        });
    }

    /**
     * Refresca la lista de índices
     */
    @FXML
    private void onRefreshIndices() {
        refreshIndices();
    }

    /**
     * Carga los índices disponibles
     */
    private void refreshIndices() {
        if (!isConnected) {
            updateStatus("❌ No hay conexión a Elasticsearch");
            return;
        }

        updateStatus("🔄 Cargando índices...");
        
        CompletableFuture.runAsync(() -> {
            try {
                List<String> indices = elasticsearchService.listAllIndices();
                
                // Filtrar índices del sistema
                List<String> userIndices = indices.stream()
                    .filter(index -> !index.startsWith("."))
                    .toList();
                
                Platform.runLater(() -> {
                    cmbIndices.getItems().clear();
                    cmbIndices.getItems().addAll(userIndices);
                    
                    if (!userIndices.isEmpty()) {
                        updateStatus("✅ " + userIndices.size() + " índices encontrados");
                        // Seleccionar el primer índice si existe excel_ventas
                        if (userIndices.contains("excel_ventas")) {
                            cmbIndices.setValue("excel_ventas");
                        }
                    } else {
                        updateStatus("⚠️ No se encontraron índices de usuario");
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("❌ Error cargando índices: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Carga los datos del índice seleccionado
     */
    @FXML
    private void onLoadData() {
        String selectedIndex = cmbIndices.getValue();
        if (selectedIndex == null) {
            updateStatus("⚠️ Selecciona un índice primero");
            return;
        }

        updateStatus("🔄 Cargando datos de " + selectedIndex + "...");
        btnLoadData.setDisable(true);
        
        CompletableFuture.runAsync(() -> {
            try {
                // Cargar hasta 100 documentos
                List<Map<String, Object>> documents = elasticsearchService.searchDocuments(selectedIndex, 100);
                
                Platform.runLater(() -> {
                    documentsData.clear();
                    documentsData.addAll(documents);
                    
                    btnLoadData.setDisable(false);
                    btnClearData.setDisable(false);
                    
                    updateStatus("✅ " + documents.size() + " documentos cargados");
                    lblLoadedCount.setText(documents.size() + " documentos cargados");
                    
                    // Scroll to top
                    if (!documents.isEmpty()) {
                        tblDocuments.scrollTo(0);
                    }
                });
                
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnLoadData.setDisable(false);
                    updateStatus("❌ Error cargando datos: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Limpia los datos de la tabla
     */
    @FXML
    private void onClearData() {
        documentsData.clear();
        btnClearData.setDisable(true);
        lblLoadedCount.setText("0 documentos cargados");
        updateStatus("🗑️ Datos limpiados");
    }

    /**
     * Actualiza el mensaje de estado
     */
    private void updateStatus(String message) {
        Platform.runLater(() -> {
            lblStatus.setText(message);
        });
    }

    /**
     * Limpia recursos al cerrar
     */
    public void cleanup() {
        if (elasticsearchService != null) {
            elasticsearchService.close();
        }
    }
}