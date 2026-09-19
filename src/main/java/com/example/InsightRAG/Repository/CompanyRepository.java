package com.example.InsightRAG.Repository;

import com.example.InsightRAG.Model.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}