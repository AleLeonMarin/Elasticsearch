package com.una.ale.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelReader {

    public ExcelReader() {
        // Constructor vacío
    }

    /**
     * Lee un archivo Excel y retorna los datos en un Map
     * @param filePath Ruta al archivo Excel
     * @return Map con los datos del Excel (fila -> lista de valores)
     * @throws IOException Si hay problemas leyendo el archivo
     */
    public Map<Integer, List<String>> readExcel(String filePath) throws IOException {
        Map<Integer, List<String>> data = new HashMap<>();
        
        try (FileInputStream excelFile = new FileInputStream(new File(filePath));
             Workbook workbook = new XSSFWorkbook(excelFile)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowIndex = 0;

            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                
                for (Cell cell : row) {
                    String cellValue = getCellValueAsString(cell);
                    rowData.add(cellValue);
                }
                
                data.put(rowIndex, rowData);
                rowIndex++;
            }
            
        } catch (FileNotFoundException e) {
            System.err.println("Archivo no encontrado: " + filePath);
            throw e;
        } catch (IOException e) {
            System.err.println("Error leyendo el archivo Excel: " + e.getMessage());
            throw e;
        }
        
        return data;
    }

    /**
     * Convierte el valor de una celda a String según su tipo
     * @param cell La celda a procesar
     * @return El valor de la celda como String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Verificar si es una fecha
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    // Convertir número a string sin notación científica
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Evaluar la fórmula y retornar el resultado
                try {
                    return getCellValueAsString(cell.getCachedFormulaResultType(), cell);
                } catch (Exception e) {
                    return "ERROR_FORMULA";
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * Método auxiliar para obtener el valor de una fórmula evaluada
     */
    private String getCellValueAsString(CellType cellType, Cell cell) {
        switch (cellType) {
            case STRING:
                return cell.getRichStringCellValue().getString();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /**
     * Método para imprimir los datos leídos (útil para debugging)
     * @param data Los datos del Excel
     */
    public void printData(Map<Integer, List<String>> data) {
        for (Map.Entry<Integer, List<String>> entry : data.entrySet()) {
            System.out.println("Fila " + entry.getKey() + ": " + entry.getValue());
        }
    }

    /**
     * Obtiene los headers (primera fila) del Excel
     * @param filePath Ruta al archivo Excel
     * @return Lista con los headers
     * @throws IOException Si hay problemas leyendo el archivo
     */
    public List<String> getHeaders(String filePath) throws IOException {
        Map<Integer, List<String>> data = readExcel(filePath);
        return data.get(0); // Retorna la primera fila como headers
    }

    /**
     * Método utilitario para indexar directamente en Elasticsearch
     * @param filePath Ruta al archivo Excel
     * @param elasticsearchService Servicio de Elasticsearch
     * @param indexName Nombre del índice donde indexar
     * @return Número de documentos indexados
     */
    public int indexToElasticsearch(String filePath, Object elasticsearchService, String indexName) {
        try {
            // Leer datos del Excel
            Map<Integer, List<String>> data = readExcel(filePath);
            
            if (data.isEmpty()) {
                System.err.println("❌ No se encontraron datos en el archivo Excel");
                return 0;
            }
            
            // Obtener headers
            List<String> headers = data.get(0);
            
            System.out.println("📊 Preparando indexación:");
            System.out.println("   - Archivo: " + filePath);
            System.out.println("   - Headers: " + headers);
            System.out.println("   - Filas de datos: " + (data.size() - 1));
            System.out.println("   - Índice destino: " + indexName);
            
            // Usar reflexión para llamar al método indexExcelData
            var method = elasticsearchService.getClass().getMethod(
                "indexExcelData", 
                Map.class, 
                Map.class, 
                List.class
            );
            
            return (Integer) method.invoke(elasticsearchService, indexName, data, headers);
            
        } catch (IOException e) {
            System.err.println("❌ Error leyendo Excel: " + e.getMessage());
            return 0;
        } catch (Exception e) {
            System.err.println("❌ Error en indexación: " + e.getMessage());
            return 0;
        }
    }
}
