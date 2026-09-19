package com.example.InsightRAG.Controller;

import com.example.InsightRAG.Model.Company;
import com.example.InsightRAG.Service.Company_Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/companies")
@CrossOrigin(origins = "*")
public class Company_Controller {
    @Autowired
    Company_Service company_service;

    @PostMapping("/upload/company")
    public Company uploadCompany(@RequestBody Company company) {
        return company_service.uploadCompany(company);
    }

    @GetMapping("/retrieve/company")
    public List<Company> retrieveCompany() {
        return company_service.retrieveCompany();
    }

}
