package com.una.ale.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.una.ale.util.ElasticConnection;

/**
 * Servicio que demuestra el uso correcto de la conexión a Elasticsearch
 */
public class ElasticsearchService {

    private ElasticConnection connection;

    public ElasticsearchService() {
        this.connection = new ElasticConnection();
    }

    /**
     * Prueba la conexión a Elasticsearch
     * @return true si la conexión es exitosa, false en caso contrario
     */
    public boolean testConnection() {
        try {
            ElasticsearchClient client = connection.connect();
            InfoResponse info = client.info();
            System.out.println("Conectado a Elasticsearch: " + info.version().number());
            return true;
        } catch (IOException e) {
            System.err.println("Error al conectar con Elasticsearch: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene información del cluster de Elasticsearch
     * @return String con información del cluster o null en caso de error
     */
    public String getClusterInfo() {
        try {
            ElasticsearchClient client = connection.connect();
            InfoResponse info = client.info();
            return String.format("Cluster: %s, Versión: %s, Lucene: %s", 
                    info.clusterName(), 
                    info.version().number(), 
                    info.version().luceneVersion());
        } catch (IOException e) {
            System.err.println("Error al obtener información del cluster: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cierra la conexión y libera recursos
     */
    public void close() {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Indexa un documento individual en Elasticsearch
     * @param indexName Nombre del índice
     * @param document Mapa con los datos del documento
     * @return ID del documento indexado o null si hay error
     */
    public String indexDocument(String indexName, Map<String, Object> document) {
        try {
            ElasticsearchClient client = connection.connect();
            
            IndexResponse response = client.index(i -> i
                .index(indexName)
                .document(document)
            );
            
            System.out.println("✅ Documento indexado: " + response.id());
            return response.id();
            
        } catch (IOException e) {
            System.err.println("❌ Error indexando documento: " + e.getMessage());
            return null;
        }
    }

    /**
     * Indexa un documento con ID específico
     * @param indexName Nombre del índice
     * @param documentId ID del documento
     * @param document Mapa con los datos del documento
     * @return true si se indexó correctamente
     */
    public boolean indexDocumentWithId(String indexName, String documentId, Map<String, Object> document) {
        try {
            ElasticsearchClient client = connection.connect();
            
            IndexResponse response = client.index(i -> i
                .index(indexName)
                .id(documentId)
                .document(document)
            );
            
            System.out.println("✅ Documento indexado con ID: " + response.id());
            return true;
            
        } catch (IOException e) {
            System.err.println("❌ Error indexando documento con ID: " + e.getMessage());
            return false;
        }
    }

    /**
     * Indexa múltiples documentos usando Bulk API (más eficiente)
     * @param indexName Nombre del índice
     * @param documents Lista de documentos a indexar
     * @return Número de documentos indexados exitosamente
     */
    @SuppressWarnings("null")
    public int bulkIndexDocuments(String indexName, List<Map<String, Object>> documents) {
        try {
            ElasticsearchClient client = connection.connect();
            
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            
            for (Map<String, Object> document : documents) {
                bulkBuilder.operations(op -> op
                    .index(idx -> idx
                        .index(indexName)
                        .document(document)
                    )
                );
            }
            
            BulkResponse bulkResponse = client.bulk(bulkBuilder.build());
            
            int successCount = 0;
            int errorCount = 0;
            
            for (BulkResponseItem item : bulkResponse.items()) {
                if (item.error() != null) {
                    errorCount++;
                    System.err.println("❌ Error en documento: " + item.error().reason());
                } else {
                    successCount++;
                }
            }
            
            System.out.println("✅ Bulk indexing completado:");
            System.out.println("   - Exitosos: " + successCount);
            System.out.println("   - Errores: " + errorCount);
            
            return successCount;
            
        } catch (IOException e) {
            System.err.println("❌ Error en bulk indexing: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Indexa datos de Excel en Elasticsearch
     * @param indexName Nombre del índice
     * @param excelData Datos del Excel (Map con filas)
     * @param headers Lista de headers para mapear columnas
     * @return Número de documentos indexados
     */
    @SuppressWarnings("null")
    public int indexExcelData(String indexName, Map<Integer, List<String>> excelData, List<String> headers) {
        try {
            ElasticsearchClient client = connection.connect();
            
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            
            // Procesar cada fila (saltando headers en la fila 0)
            for (int rowIndex = 1; rowIndex < excelData.size(); rowIndex++) {
                List<String> rowData = excelData.get(rowIndex);
                
                // Crear documento mapeando headers con datos
                Map<String, Object> document = new HashMap<>();
                for (int colIndex = 0; colIndex < headers.size() && colIndex < rowData.size(); colIndex++) {
                    String fieldName = headers.get(colIndex);
                    String fieldValue = rowData.get(colIndex);
                    
                    // Limpiar nombre del campo (remover espacios, caracteres especiales)
                    fieldName = fieldName.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
                    
                    document.put(fieldName, fieldValue);
                }
                
                // Agregar metadata
                document.put("row_number", rowIndex);
                document.put("indexed_at", java.time.Instant.now().toString());
                
                bulkBuilder.operations(op -> op
                    .index(idx -> idx
                        .index(indexName)
                        .document(document)
                    )
                );
            }
            
            BulkResponse bulkResponse = client.bulk(bulkBuilder.build());
            
            int successCount = 0;
            int errorCount = 0;
            
            for (BulkResponseItem item : bulkResponse.items()) {
                if (item.error() != null) {
                    errorCount++;
                    System.err.println("❌ Error en fila: " + item.error().reason());
                } else {
                    successCount++;
                }
            }
            
            System.out.println("📊 Indexación de Excel completada:");
            System.out.println("   - Índice: " + indexName);
            System.out.println("   - Documentos exitosos: " + successCount);
            System.out.println("   - Errores: " + errorCount);
            
            return successCount;
            
        } catch (IOException e) {
            System.err.println("❌ Error indexando datos de Excel: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Ejemplo de uso con try-with-resources
     */
    public static void ejemploDeUso() {
        ElasticsearchService service = new ElasticsearchService();
        try {
            boolean connected = service.testConnection();
            if (connected) {
                String info = service.getClusterInfo();
                System.out.println("Información del cluster: " + info);
            }
        } finally {
            service.close();
        }
    }

    /**
     * Cuenta el número de documentos en un índice
     * @param indexName Nombre del índice
     * @return Número de documentos o -1 si hay error
     */
    public long countDocuments(String indexName) {
        try {
            ElasticsearchClient client = connection.connect();
            
            CountResponse countResponse = client.count(c -> c.index(indexName));
            
            System.out.println("📊 Documentos en índice '" + indexName + "': " + countResponse.count());
            return countResponse.count();
            
        } catch (IOException e) {
            System.err.println("❌ Error contando documentos: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Busca y retorna documentos de un índice
     * @param indexName Nombre del índice
     * @param size Número máximo de documentos a retornar
     * @return Lista de documentos o lista vacía si hay error
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<Map<String, Object>> searchDocuments(String indexName, int size) {
        List<Map<String, Object>> documents = new ArrayList<>();
        
        try {
            ElasticsearchClient client = connection.connect();
            
            SearchResponse<Map> searchResponse = client.search(s -> s
                .index(indexName)
                .size(size)
                .query(q -> q.matchAll(m -> m))
            , Map.class);
            
            System.out.println("🔍 Encontrados " + searchResponse.hits().hits().size() + " documentos");
            
            for (Hit<Map> hit : searchResponse.hits().hits()) {
                Map<String, Object> document = new HashMap<>();
                document.put("_id", hit.id());
                document.put("_index", hit.index());
                
                if (hit.source() != null) {
                    document.putAll(hit.source());
                }
                
                documents.add(document);
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error buscando documentos: " + e.getMessage());
        }
        
        return documents;
    }

    /**
     * Obtiene información de un índice específico
     * @param indexName Nombre del índice
     * @return Información del índice como String
     */
    public String getIndexInfo(String indexName) {
        try {
            ElasticsearchClient client = connection.connect();
            
            // Contar documentos
            long docCount = countDocuments(indexName);
            
            // Obtener información del índice
            GetIndexResponse indexResponse = client.indices().get(g -> g.index(indexName));
            
            StringBuilder info = new StringBuilder();
            info.append("📋 Información del Índice: ").append(indexName).append("\n");
            info.append("📊 Documentos: ").append(docCount).append("\n");
            info.append("🏷️ Aliases: ").append(indexResponse.result().get(indexName).aliases().keySet()).append("\n");
            
            return info.toString();
            
        } catch (IOException e) {
            System.err.println("❌ Error obteniendo información del índice: " + e.getMessage());
            return "Error obteniendo información del índice: " + e.getMessage();
        }
    }

    /**
     * Lista todos los índices disponibles en Elasticsearch
     * @return Lista de nombres de índices
     */
    public List<String> listAllIndices() {
        List<String> indices = new ArrayList<>();
        
        try {
            ElasticsearchClient client = connection.connect();
            
            GetIndexResponse response = client.indices().get(g -> g.index("*"));
            
            indices.addAll(response.result().keySet());
            
            System.out.println("📋 Índices encontrados: " + indices);
            
        } catch (IOException e) {
            System.err.println("❌ Error listando índices: " + e.getMessage());
        }
        
        return indices;
    }

    /**
     * Imprime los primeros documentos de un índice de forma legible
     * @param indexName Nombre del índice
     * @param maxDocuments Número máximo de documentos a mostrar
     */
    public void printIndexContent(String indexName, int maxDocuments) {
        try {
            System.out.println("\n📋 === CONTENIDO DEL ÍNDICE: " + indexName + " ===");
            
            // Información general
            long totalDocs = countDocuments(indexName);
            System.out.println("📊 Total de documentos: " + totalDocs);
            
            // Obtener documentos
            List<Map<String, Object>> documents = searchDocuments(indexName, maxDocuments);
            
            if (documents.isEmpty()) {
                System.out.println("❌ No se encontraron documentos");
                return;
            }
            
            // Mostrar documentos
            for (int i = 0; i < documents.size(); i++) {
                Map<String, Object> doc = documents.get(i);
                System.out.println("\n📄 Documento " + (i + 1) + ":");
                System.out.println("   ID: " + doc.get("_id"));
                
                // Mostrar campos (excluyendo metadatos)
                doc.entrySet().stream()
                   .filter(entry -> !entry.getKey().startsWith("_"))
                   .forEach(entry -> 
                       System.out.println("   " + entry.getKey() + ": " + entry.getValue())
                   );
            }
            
            if (totalDocs > maxDocuments) {
                System.out.println("\n💡 Mostrando " + maxDocuments + " de " + totalDocs + " documentos");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error mostrando contenido del índice: " + e.getMessage());
        }
    }
}