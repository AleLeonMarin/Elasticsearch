package com.una.ale.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Clase utilitaria para crear archivos Excel de ejemplo
 */
public class ExcelCreator {
    
    /**
     * Crea un archivo Excel de ejemplo con datos de muestra
     * @param filePath Ruta donde crear el archivo
     * @throws IOException Si hay problemas creando el archivo
     */
    public static void createSampleExcel(String filePath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Datos de Ejemplo");
            
            // Crear headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Nombre", "Edad", "Email", "Activo", "Fecha_Registro", "Salario"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                
                // Estilo para headers
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }
            
            // Crear datos de ejemplo
            Object[][] sampleData = {
                {1, "Juan Pérez", 25, "juan@email.com", true, LocalDateTime.now(), 50000.0},
                {2, "María García", 30, "maria@email.com", true, LocalDateTime.now().minusDays(30), 60000.0},
                {3, "Carlos López", 28, "carlos@email.com", false, LocalDateTime.now().minusDays(60), 55000.0},
                {4, "Ana Martínez", 35, "ana@email.com", true, LocalDateTime.now().minusDays(90), 70000.0},
                {5, "Luis Rodríguez", 32, "luis@email.com", true, LocalDateTime.now().minusDays(120), 65000.0}
            };
            
            for (int i = 0; i < sampleData.length; i++) {
                Row row = sheet.createRow(i + 1);
                Object[] rowData = sampleData[i];
                
                for (int j = 0; j < rowData.length; j++) {
                    Cell cell = row.createCell(j);
                    Object value = rowData[j];
                    
                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof Integer) {
                        cell.setCellValue((Integer) value);
                    } else if (value instanceof Double) {
                        cell.setCellValue((Double) value);
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else if (value instanceof LocalDateTime) {
                        cell.setCellValue((LocalDateTime) value);
                        
                        // Formato de fecha
                        CellStyle dateStyle = workbook.createCellStyle();
                        CreationHelper createHelper = workbook.getCreationHelper();
                        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
                        cell.setCellStyle(dateStyle);
                    }
                }
            }
            
            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Guardar el archivo
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            
            System.out.println("✅ Archivo Excel creado exitosamente: " + filePath);
        }
    }
    
    public static void main(String[] args) {
        try {
            String filePath = "src/main/resources/ejemplo.xlsx";
            createSampleExcel(filePath);
            
            // Crear directorio si no existe
            java.io.File file = new java.io.File(filePath);
            file.getParentFile().mkdirs();
            
            createSampleExcel(filePath);
            
            // Probar leer el archivo creado
            ExcelReader reader = new ExcelReader();
            System.out.println("\n=== Probando lectura del archivo creado ===");
            var data = reader.readExcel(filePath);
            reader.printData(data);
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}