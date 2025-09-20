package com.una.ale.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.io.IOException;

public class ElasticConnection {

    private ElasticsearchClient client;
    private ElasticsearchTransport transport;
    private RestClient restClient;

    public ElasticConnection() {
        // Constructor vacío
    }

    /**
     * Establece la conexión con Elasticsearch
     * @return ElasticsearchClient configurado
     * @throws IOException si hay problemas al conectar
     */
    public ElasticsearchClient connect() throws IOException {
        if (client == null) {
            // Crear el cliente REST de bajo nivel
            restClient = RestClient.builder(
                    new HttpHost("localhost", 9200, "http")
            ).build();

            // Crear el transporte con el mapper JSON
            transport = new RestClientTransport(
                    restClient, new JacksonJsonpMapper()
            );

            // Crear el cliente de Elasticsearch
            client = new ElasticsearchClient(transport);
        }
        return client;
    }

    /**
     * Obtiene el cliente de Elasticsearch (debe llamar connect() primero)
     * @return ElasticsearchClient o null si no está conectado
     */
    public ElasticsearchClient getClient() {
        return client;
    }

    /**
     * Verifica si la conexión está activa
     * @return true si el cliente está disponible, false en caso contrario
     */
    public boolean isConnected() {
        return client != null;
    }

    /**
     * Cierra la conexión y libera los recursos
     */
    public void close() {
        try {
            if (transport != null) {
                transport.close();
            }
            if (restClient != null) {
                restClient.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        } finally {
            client = null;
            transport = null;
            restClient = null;
        }
    }
}
