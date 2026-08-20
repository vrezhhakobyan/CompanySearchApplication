package com.example.intern_taks;

import com.example.intern_taks.controller.SearchController;
import com.example.intern_taks.domain.Company;
import com.example.intern_taks.service.CompanySearchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchControllerTest {

    @Test
    void searchShouldReturnCompanies() {

        CompanySearchService service =
                mock(CompanySearchService.class);

        Company company = new Company();
        company.setCompanyNumber("01234567");
        company.setName("Test Company");
        company.setStatus("active");

        when(service.search("test", false))
                .thenReturn(List.of(company));

        SearchController controller =
                new SearchController(service);

        var response =
                controller.search("test", false);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(1, response.getBody().size());

        Company result =
                response.getBody().get(0);

        assertEquals(
                "01234567",
                result.getCompanyNumber()
        );

        assertEquals(
                "Test Company",
                result.getName()
        );

        assertEquals(
                "active",
                result.getStatus()
        );

        verify(service)
                .search("test", false);
    }

    @Test
    void searchShouldForwardForceRefresh() {

        CompanySearchService service =
                mock(CompanySearchService.class);

        when(service.search("tesla", true))
                .thenReturn(List.of());

        SearchController controller =
                new SearchController(service);

        var response = controller.search("tesla", true);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(List.of(), response.getBody());
        verify(service).search("tesla", true);
    }
}
