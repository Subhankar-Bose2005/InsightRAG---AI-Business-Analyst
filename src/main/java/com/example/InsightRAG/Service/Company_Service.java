package com.example.InsightRAG.Service;

import com.example.InsightRAG.Model.Company;
import com.example.InsightRAG.Repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class Company_Service {
    @Autowired
    CompanyRepository company_repo;

    public Company uploadCompany(Company company) {
        return company_repo.save(company);
    }

    public List<Company> retrieveCompany() {
        return company_repo.findAll();
    }
}
