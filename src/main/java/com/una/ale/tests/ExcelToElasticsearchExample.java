package com.una.ale.tests;

import com.una.ale.services.ElasticsearchService;
import com.una.ale.util.ExcelReader;

import java.io.IOException;
import java.util.*;

/**
 * Clase de ejemplo que demuestra cómo indexar datos de Excel en Elasticsearch
 */
public class ExcelToElasticsearchExample {
    
    public static void main(String[] args) {
        ElasticsearchService esService = new ElasticsearchService();
        ExcelReader excelReader = new ExcelReader();
        
        try {
            // 1. Verificar conexión a Elasticsearch
            System.out.println("🔍 Verificando conexión a Elasticsearch...");
            if (!esService.testConnection()) {
                System.err.println("❌ No se pudo conectar a Elasticsearch");
                return;
            }
            
            // 2. Ruta al archivo Excel
            String excelPath = "src/main/resources/com/una/ale/resources/excel/ventas.xlsx";
            String indexName = "excel_data";
            
            // 3. Leer datos del Excel
            System.out.println("📖 Leyendo archivo Excel...");
            Map<Integer, List<String>> excelData = excelReader.readExcel(excelPath);
            
            if (excelData.isEmpty()) {
                System.err.println("❌ No se encontraron datos en el Excel");
                return;
            }
            
            // 4. Obtener headers
            List<String> headers = excelData.get(0);
            System.out.println("📋 Headers encontrados: " + headers);
            System.out.println("📊 Filas de datos: " + (excelData.size() - 1));
            
            // 5. Indexar en Elasticsearch
            System.out.println("📤 Indexando datos en Elasticsearch...");
            int indexedCount = esService.indexExcelData(indexName, excelData, headers);
            
            if (indexedCount > 0) {
                System.out.println("✅ Indexación completada exitosamente!");
                System.out.println("📈 Documentos indexados: " + indexedCount);
            } else {
                System.err.println("❌ No se pudieron indexar los datos");
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error de I/O: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Error inesperado: " + e.getMessage());
            e.printStackTrace();
        } finally {
            esService.close();
        }
    }
    
    /**
     * Ejemplo de indexación de documento individual
     */
    public static void indexSingleDocumentExample() {
        ElasticsearchService esService = new ElasticsearchService();
        
        try {
            // Crear un documento de ejemplo
            Map<String, Object> document = new HashMap<>();
            document.put("nombre", "Juan Pérez");
            document.put("edad", 30);
            document.put("email", "juan@email.com");
            document.put("activo", true);
            document.put("salario", 50000.0);
            document.put("fecha_registro", java.time.Instant.now().toString());
            
            // Indexar el documento
            String documentId = esService.indexDocument("empleados", document);
            
            if (documentId != null) {
                System.out.println("✅ Documento indexado con ID: " + documentId);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        } finally {
            esService.close();
        }
    }
    
    /**
     * Ejemplo de indexación masiva (bulk)
     */
    public static void bulkIndexExample() {
        ElasticsearchService esService = new ElasticsearchService();
        
        try {
            // Crear múltiples documentos
            List<Map<String, Object>> documents = new ArrayList<>();
            
            for (int i = 1; i <= 100; i++) {
                Map<String, Object> document = new HashMap<>();
                document.put("id", i);
                document.put("nombre", "Usuario " + i);
                document.put("email", "usuario" + i + "@email.com");
                document.put("activo", i % 2 == 0);
                documents.add(document);
            }
            
            // Indexar en bulk
            int indexedCount = esService.bulkIndexDocuments("usuarios", documents);
            System.out.println("✅ Bulk indexing completado: " + indexedCount + " documentos");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
        } finally {
            esService.close();
        }
    }
}