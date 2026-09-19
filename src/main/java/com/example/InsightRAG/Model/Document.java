package com.example.InsightRAG.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String filename;

    private String fileType;

    private String filePath;

    private String status;

    private LocalDateTime uploadedAt;

    public Document() {
    }

    public Long getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getFilename() {
        return filename;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}