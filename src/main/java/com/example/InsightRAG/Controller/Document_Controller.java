package com.example.InsightRAG.Controller;

import com.example.InsightRAG.Model.Document;
import com.example.InsightRAG.Service.Document_Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = "*")
public class Document_Controller {

    @Autowired
    Document_Service doc_service;


    @PostMapping("/upload/{companyId}")
    public ResponseEntity<Document> uploadDocument(
            @PathVariable("companyId") Long companyId,
            @RequestParam("file") MultipartFile file) throws IOException {

        return ResponseEntity.ok(
                doc_service.uploadDocument(companyId, file)
        );
    }


    @GetMapping
    public ResponseEntity<List<Document>> getDocuments() {

        return ResponseEntity.ok(
                doc_service.getDocuments()
        );
    }


    @PostMapping("/process/{documentId}")
    public ResponseEntity<String> processDocument(
            @PathVariable("documentId") Long documentId) throws IOException {

        return ResponseEntity.ok(
                doc_service.processDocument(documentId)
        );
    }


    @PostMapping("/extract/{documentId}")
    public ResponseEntity<String> extractDocument(
            @PathVariable("documentId") Long documentId) throws IOException {

        return ResponseEntity.ok(
                doc_service.extractDocument(documentId)
        );
    }


    @PostMapping("/embeddings/{documentId}")
    public ResponseEntity<String> embedDocument(
            @PathVariable("documentId") Long documentId) throws IOException {

        return ResponseEntity.ok(
                doc_service.embedDocument(documentId)
        );
    }


    @DeleteMapping("/{documentId}")
    public ResponseEntity<String> deleteDocument(
            @PathVariable("documentId") Long documentId) {

        try {

            doc_service.deleteDocument(documentId);

            return ResponseEntity.ok(
                    "Document deleted successfully"
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }
}