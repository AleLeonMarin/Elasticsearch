package com.una.ale.tests;

import com.una.ale.services.ElasticsearchService;

/**
 * Clase simple para probar la conexión a Elasticsearch sin interfaz gráfica
 */
public class ElasticsearchTest {
    
    public static void main(String[] args) {
        System.out.println("=== Prueba de Conexión a Elasticsearch ===");
        
        ElasticsearchService service = new ElasticsearchService();
        
        try {
            System.out.println("Intentando conectar a Elasticsearch...");
            
            boolean connected = service.testConnection();
            
            if (connected) {
                System.out.println("✅ Conexión exitosa!");
                
                String info = service.getClusterInfo();
                System.out.println("📊 " + info);
                
            } else {
                System.out.println("❌ No se pudo conectar a Elasticsearch");
                System.out.println("Asegúrate de que Elasticsearch esté ejecutándose en localhost:9200");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error durante la prueba: " + e.getMessage());
            System.out.println("\n💡 Para iniciar Elasticsearch con Docker:");
            System.out.println("docker run -d --name elasticsearch \\");
            System.out.println("  -p 9200:9200 \\");
            System.out.println("  -e \"discovery.type=single-node\" \\");
            System.out.println("  -e \"xpack.security.enabled=false\" \\");
            System.out.println("  docker.elastic.co/elasticsearch/elasticsearch:8.11.0");
            
        } finally {
            service.close();
            System.out.println("🔒 Conexión cerrada");
        }
    }
}