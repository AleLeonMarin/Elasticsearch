package com.una.ale.tests;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.una.ale.services.ElasticsearchService;
import com.una.ale.util.ExcelReader;

/**
 * Clase de ejemplo para demostrar el uso del ExcelReader
 */
public class ExcelReaderExample {
    
    public static void main(String[] args) {
        ExcelReader reader = new ExcelReader();
        
        // Ejemplo de uso - cambiar la ruta por un archivo Excel real
        String excelPath = "src/main/resources/ejemplo.xlsx";
        
        try {
            System.out.println("=== Leyendo archivo Excel ===");
            System.out.println("Archivo: " + excelPath);
            
            // Leer todos los datos
            Map<Integer, List<String>> data = reader.readExcel(excelPath);
            
            System.out.println("Total de filas leídas: " + data.size());
            
            // Mostrar los headers (primera fila)
            if (!data.isEmpty()) {
                List<String> headers = data.get(0); 
                System.out.println("Headers: " + headers);
                
                // Mostrar algunas filas de datos
                System.out.println("\n=== Primeras 5 filas ===");
                for (int i = 0; i < Math.min(5, data.size()); i++) {
                    System.out.println("Fila " + i + ": " + data.get(i));
                }
            }
            
            // Alternativa: obtener solo los headers
            List<String> headers = reader.getHeaders(excelPath);
            System.out.println("\nHeaders usando getHeaders(): " + headers);
            
        } catch (IOException e) {
            System.err.println("Error leyendo el archivo Excel:");
            System.err.println("- Verifica que el archivo existe en la ruta especificada");
            System.err.println("- Asegúrate de que el archivo no esté abierto en Excel");
            System.err.println("- Verifica que tienes permisos de lectura en el archivo");
            System.err.println("Error: " + e.getMessage());
            
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Método para demostrar cómo integrar con Elasticsearch
     */
    public static void exampleWithElasticsearch() {
        ExcelReader reader = new ExcelReader();
        ElasticsearchService esService = new ElasticsearchService();
        
        try {
            // Leer datos del Excel
            Map<Integer, List<String>> excelData = reader.readExcel("data.xlsx");
            
            // Obtener headers
            List<String> headers = excelData.get(0);
            
            // Procesar cada fila (saltando los headers)
            for (int i = 1; i < excelData.size(); i++) {
                List<String> rowData = excelData.get(i);
                
                // Crear un documento JSON para Elasticsearch
                // Aquí podrías mapear los datos del Excel a un formato JSON
                // y enviarlos a Elasticsearch
                
                System.out.println("Fila " + i + " lista para indexar en Elasticsearch");
                System.out.println("Headers: " + headers);
                System.out.println("Datos: " + rowData);
            }
            
        } catch (IOException e) {
            System.err.println("Error procesando Excel para Elasticsearch: " + e.getMessage());
        } finally {
            esService.close();
        }
    }
}