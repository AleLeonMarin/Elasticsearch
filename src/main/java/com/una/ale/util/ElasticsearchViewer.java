package com.una.ale.util;

import com.una.ale.services.ElasticsearchService;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Utilidad para visualizar y explorar datos indexados en Elasticsearch
 */
public class ElasticsearchViewer {
    
    private final ElasticsearchService esService;
    
    public ElasticsearchViewer() {
        this.esService = new ElasticsearchService();
    }
    
    /**
     * Método principal para exploración interactiva
     */
    public static void main(String[] args) {
        ElasticsearchViewer viewer = new ElasticsearchViewer();
        
        try {
            if (!viewer.esService.testConnection()) {
                System.err.println("❌ No se pudo conectar a Elasticsearch");
                return;
            }
            
            viewer.showInteractiveMenu();
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        } finally {
            viewer.esService.close();
        }
    }
    
    /**
     * Muestra un menú interactivo para explorar Elasticsearch
     */
    private void showInteractiveMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean continuar = true;
        
        while (continuar) {
            System.out.println("\n🔍 === ELASTICSEARCH VIEWER ===");
            System.out.println("1. Listar todos los índices");
            System.out.println("2. Ver contenido de un índice");
            System.out.println("3. Contar documentos en un índice");
            System.out.println("4. Información del cluster");
            System.out.println("5. Ver índice 'excel_ventas' (por defecto)");
            System.out.println("0. Salir");
            System.out.print("\nSelecciona una opción: ");
            
            try {
                int opcion = Integer.parseInt(scanner.nextLine());
                
                switch (opcion) {
                    case 1:
                        listarIndices();
                        break;
                    case 2:
                        System.out.print("Nombre del índice: ");
                        String indexName = scanner.nextLine();
                        verContenidoIndice(indexName);
                        break;
                    case 3:
                        System.out.print("Nombre del índice: ");
                        String indexCount = scanner.nextLine();
                        contarDocumentos(indexCount);
                        break;
                    case 4:
                        mostrarInfoCluster();
                        break;
                    case 5:
                        verContenidoIndice("excel_ventas");
                        break;
                    case 0:
                        continuar = false;
                        System.out.println("👋 ¡Hasta luego!");
                        break;
                    default:
                        System.out.println("❌ Opción no válida");
                }
                
            } catch (NumberFormatException e) {
                System.out.println("❌ Por favor ingresa un número válido");
            }
        }
        
        scanner.close();
    }
    
    /**
     * Lista todos los índices disponibles
     */
    private void listarIndices() {
        System.out.println("\n📋 === ÍNDICES DISPONIBLES ===");
        List<String> indices = esService.listAllIndices();
        
        if (indices.isEmpty()) {
            System.out.println("❌ No se encontraron índices");
        } else {
            for (int i = 0; i < indices.size(); i++) {
                String indexName = indices.get(i);
                long docCount = esService.countDocuments(indexName);
                System.out.printf("%d. %s (%d documentos)%n", i + 1, indexName, docCount);
            }
        }
    }
    
    /**
     * Muestra el contenido de un índice específico
     */
    private void verContenidoIndice(String indexName) {
        System.out.println("\n📄 === CONTENIDO DEL ÍNDICE: " + indexName + " ===");
        
        // Mostrar información general
        String indexInfo = esService.getIndexInfo(indexName);
        System.out.println(indexInfo);
        
        // Mostrar documentos
        esService.printIndexContent(indexName, 10);
    }
    
    /**
     * Cuenta documentos en un índice
     */
    private void contarDocumentos(String indexName) {
        System.out.println("\n📊 === CONTEO DE DOCUMENTOS ===");
        long count = esService.countDocuments(indexName);
        
        if (count >= 0) {
            System.out.printf("Índice '%s' contiene %d documentos%n", indexName, count);
        }
    }
    
    /**
     * Muestra información del cluster
     */
    private void mostrarInfoCluster() {
        System.out.println("\n🏥 === INFORMACIÓN DEL CLUSTER ===");
        String clusterInfo = esService.getClusterInfo();
        System.out.println(clusterInfo);
    }
    
    /**
     * Método rápido para ver datos indexados (sin interacción)
     */
    public static void quickView() {
        ElasticsearchViewer viewer = new ElasticsearchViewer();
        
        try {
            if (!viewer.esService.testConnection()) {
                System.err.println("❌ No se pudo conectar a Elasticsearch");
                return;
            }
            
            System.out.println("🔍 === VISTA RÁPIDA DE ELASTICSEARCH ===");
            
            // Información del cluster
            System.out.println("\n🏥 Cluster Info:");
            System.out.println(viewer.esService.getClusterInfo());
            
            // Listar índices
            System.out.println("\n📋 Índices disponibles:");
            List<String> indices = viewer.esService.listAllIndices();
            
            for (String index : indices) {
                long count = viewer.esService.countDocuments(index);
                System.out.printf("  - %s: %d documentos%n", index, count);
            }
            
            // Mostrar contenido del índice principal si existe
            if (indices.contains("excel_ventas")) {
                System.out.println("\n📄 Contenido de 'excel_ventas':");
                viewer.esService.printIndexContent("excel_ventas", 5);
            } else if (indices.contains("excel_data")) {
                System.out.println("\n📄 Contenido de 'excel_data':");
                viewer.esService.printIndexContent("excel_data", 5);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        } finally {
            viewer.esService.close();
        }
    }
    
    /**
     * Busca documentos que contengan un texto específico
     */
    public void buscarTexto(String indexName, String texto) {
        System.out.println("\n🔍 === BÚSQUEDA DE TEXTO ===");
        System.out.println("Índice: " + indexName);
        System.out.println("Texto: " + texto);
        
        List<Map<String, Object>> documentos = esService.searchDocuments(indexName, 20);
        
        // Filtrar documentos que contengan el texto
        long matches = documentos.stream()
            .filter(doc -> doc.values().stream()
                .anyMatch(value -> value.toString().toLowerCase()
                    .contains(texto.toLowerCase())))
            .count();
            
        System.out.println("✅ Documentos que contienen '" + texto + "': " + matches);
    }
}