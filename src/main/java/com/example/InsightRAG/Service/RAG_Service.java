package com.example.InsightRAG.Service;

import com.example.InsightRAG.Model.RAG_Request;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Service
public class RAG_Service {

    public String retrieval(RAG_Request request) throws IOException {
        RestClient restClient = RestClient.create();
        return restClient.post().uri("http://localhost:8000/ask")
                .contentType(MediaType.APPLICATION_JSON).body(request)
                .retrieve().body(String.class);
    }
}
