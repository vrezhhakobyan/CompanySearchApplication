package com.example.intern_taks.controller;

import com.example.intern_taks.domain.Company;
import com.example.intern_taks.service.CompanySearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final CompanySearchService companySearchService;

    public SearchController(
            CompanySearchService companySearchService
    ) {
        this.companySearchService = companySearchService;
    }

    @GetMapping
    public ResponseEntity<List<Company>> search(
            @RequestParam("q") String query,
            @RequestParam(
                    value = "forceRefresh",
                    defaultValue = "false"
            )
            boolean forceRefresh
    ) {

        List<Company> companies =
                companySearchService.search(
                        query,
                        forceRefresh
                );

        return ResponseEntity.ok(companies);
    }
}