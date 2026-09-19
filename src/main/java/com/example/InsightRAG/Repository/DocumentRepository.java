package com.example.InsightRAG.Repository;
import com.example.InsightRAG.Model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
