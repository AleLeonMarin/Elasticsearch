package com.una.ale.util;

import com.una.ale.services.ElasticsearchService;

/**
 * Utilidad simple para ver datos indexados sin menú interactivo
 */
public class QuickViewElasticsearch {
    
    public static void main(String[] args) {
        ElasticsearchService esService = new ElasticsearchService();
        
        try {
            System.out.println("🔍 === VISTA RÁPIDA DE ELASTICSEARCH ===");
            
            // Verificar conexión
            if (!esService.testConnection()) {
                System.err.println("❌ No se pudo conectar a Elasticsearch");
                return;
            }
            
            // Información del cluster
            System.out.println("\n🏥 === INFORMACIÓN DEL CLUSTER ===");
            System.out.println(esService.getClusterInfo());
            
            // Listar todos los índices
            System.out.println("\n📋 === ÍNDICES DISPONIBLES ===");
            var indices = esService.listAllIndices();
            
            if (indices.isEmpty()) {
                System.out.println("❌ No se encontraron índices");
                return;
            }
            
            // Mostrar información de cada índice
            for (String index : indices) {
                if (!index.startsWith(".")) { // Ignorar índices del sistema
                    long count = esService.countDocuments(index);
                    System.out.printf("📊 %s: %d documentos%n", index, count);
                }
            }
            
            // Mostrar contenido detallado del índice principal
            String mainIndex = "excel_ventas";
            if (indices.contains(mainIndex)) {
                System.out.println("\n📄 === CONTENIDO DE '" + mainIndex + "' ===");
                esService.printIndexContent(mainIndex, 5);
            } else if (indices.contains("excel_data")) {
                System.out.println("\n📄 === CONTENIDO DE 'excel_data' ===");
                esService.printIndexContent("excel_data", 5);
            } else {
                // Mostrar el primer índice que no sea del sistema
                for (String index : indices) {
                    if (!index.startsWith(".")) {
                        System.out.println("\n📄 === CONTENIDO DE '" + index + "' ===");
                        esService.printIndexContent(index, 5);
                        break;
                    }
                }
            }
            
            System.out.println("\n✅ Vista rápida completada");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            esService.close();
        }
    }
}