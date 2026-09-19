package com.example.InsightRAG.Controller;

import com.example.InsightRAG.Model.RAG_Request;
import com.example.InsightRAG.Service.RAG_Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/rag")
@CrossOrigin(origins = "*")
public class RAG_Controller {
    @Autowired
    RAG_Service rag_service;

    @PostMapping("/ask")
    public ResponseEntity<String> retrieveRag(@RequestBody RAG_Request request) throws IOException {
        return ResponseEntity.ok(rag_service.retrieval(request));
    }
}
