package com.example.InsightRAG.Service;
import com.example.InsightRAG.Model.Document;
import com.example.InsightRAG.Model.Company;
import com.example.InsightRAG.Repository.CompanyRepository;
import com.example.InsightRAG.Repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class Document_Service {
    @Autowired
    DocumentRepository doc_repo;
    @Autowired
    CompanyRepository company_repo;
    public Document uploadDocument(Long companyId, MultipartFile file) throws IOException {


        Company company = company_repo.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        Path uploadDirectory = Paths.get("uploads");
        Files.createDirectories(uploadDirectory);
        Path filePath = uploadDirectory.resolve(file.getOriginalFilename());
        Files.write(filePath, file.getBytes());

        Document document = new Document();
        document.setCompany(company);
        document.setFilename(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setFilePath(filePath.toString());
        document.setStatus("UPLOADED");
        document.setUploadedAt(LocalDateTime.now());

        return doc_repo.save(document);
    }

    public String processDocument(Long documentId) throws IOException {
        Document document = doc_repo.findById(documentId).orElseThrow(() -> new RuntimeException("Document not found"));

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found");
        }

        FileSystemResource fileResource = new FileSystemResource(filePath);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        RestClient restClient = RestClient.create();
        return restClient.post().uri("http://localhost:8000/process-document")
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(String.class);
    }

    public String extractDocument(Long documentId) throws IOException {


        Document document = doc_repo.findById(documentId).orElseThrow(() -> new RuntimeException("Document not found"));

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found");
        }

        FileSystemResource fileResource = new FileSystemResource(filePath);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        RestClient restClient = RestClient.create();
        return restClient.post().uri("http://localhost:8000/extract-document")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body).retrieve().body(String.class);
    }

    public String embedDocument(Long documentId) throws IOException {


        Document document = doc_repo.findById(documentId).orElseThrow(() -> new RuntimeException("Document not found"));

        Path filePath = Paths.get(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found");
        }
        FileSystemResource fileResource = new FileSystemResource(filePath);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        RestClient restClient = RestClient.create();
        return restClient.post().uri("http://localhost:8000/create-embeddings/" + documentId)
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(String.class);
    }

    public void deleteDocument(Long documentId) throws IOException {

        Document document = doc_repo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        Path filePath = Paths.get(document.getFilePath());

        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        doc_repo.delete(document);
    }

    public List<Document> getDocuments() {

        return doc_repo.findAll();
    }
}
