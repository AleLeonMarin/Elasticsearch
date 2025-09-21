package com.una.ale.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;

import java.io.IOException;

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
}