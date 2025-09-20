package com.una.ale;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Aplicación JavaFX para el visualizador de Elasticsearch
 */
public class ElasticsearchViewerApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        try {
            // Cargar el FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/una/ale/view/elasticsearch-viewer.fxml"));
            Parent root = loader.load();
            
            // Configurar la escena
            Scene scene = new Scene(root, 1000, 700);
            stage.setTitle("Elasticsearch Data Viewer - Universidad Nacional");
            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            
            // Configurar evento de cierre
            stage.setOnCloseRequest(event -> {
                try {
                    // Limpiar recursos del controlador
                    var controller = loader.getController();
                    if (controller instanceof com.una.ale.controllers.ElasticsearchViewController) {
                        ((com.una.ale.controllers.ElasticsearchViewController) controller).cleanup();
                    }
                } catch (Exception e) {
                    System.err.println("Error limpiando recursos: " + e.getMessage());
                }
            });
            
            stage.show();
            
        } catch (IOException e) {
            System.err.println("Error cargando la interfaz: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}