package com.example.intern_taks.service;

import com.example.intern_taks.domain.Company;
import com.example.intern_taks.repository.SearchCacheRepository;
import com.example.intern_taks.scraper.CompaniesHouseScraper;
import com.example.intern_taks.storage.SearchCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanySearchServiceTest {

    private static final String CACHED_JSON =
            "[{\"companyNumber\":\"01234567\"}]";

    @Mock
    private CompaniesHouseScraper scraper;

    @Mock
    private SearchCacheRepository cacheRepository;

    @Mock
    private JsonMapper jsonMapper;

    private CompanySearchService service;

    @BeforeEach
    void setUp() {
        service = new CompanySearchService(
                scraper,
                cacheRepository,
                jsonMapper
        );
    }

    @Test
    void searchReturnsFreshCachedResultsWithoutScraping() throws Exception {
        List<Company> cachedCompanies = List.of(company("01234567"));
        SearchCache cache = new SearchCache(
                "tesla",
                CACHED_JSON,
                Instant.now().minus(1, ChronoUnit.HOURS)
        );

        when(cacheRepository.findBySearchQuery("tesla"))
                .thenReturn(Optional.of(cache));
        when(jsonMapper.readValue(
                eq(CACHED_JSON),
                any(TypeReference.class)
        )).thenReturn(cachedCompanies);

        List<Company> result = service.search(" Tesla ", false);

        assertSame(cachedCompanies, result);
        verifyNoInteractions(scraper);
        verify(cacheRepository, never()).save(any());
    }

    @Test
    void searchScrapesAndCachesWhenNoCacheExists() throws Exception {
        List<Company> scrapedCompanies = List.of(company("01234567"));

        when(cacheRepository.findBySearchQuery("tesla"))
                .thenReturn(Optional.empty());
        when(scraper.searchCompanies("tesla"))
                .thenReturn(scrapedCompanies);
        when(jsonMapper.writeValueAsString(scrapedCompanies))
                .thenReturn(CACHED_JSON);

        List<Company> result = service.search("tesla", false);

        assertSame(scrapedCompanies, result);
        ArgumentCaptor<SearchCache> cacheCaptor =
                ArgumentCaptor.forClass(SearchCache.class);
        verify(cacheRepository).save(cacheCaptor.capture());

        SearchCache savedCache = cacheCaptor.getValue();
        assertEquals("tesla", savedCache.getSearchQuery());
        assertEquals(CACHED_JSON, savedCache.getResultsJson());
        verify(scraper).searchCompanies("tesla");
    }

    @Test
    void searchRefreshesExpiredCache() throws Exception {
        SearchCache expiredCache = new SearchCache(
                "tesla",
                CACHED_JSON,
                Instant.now().minus(25, ChronoUnit.HOURS)
        );
        List<Company> scrapedCompanies = List.of(company("01234567"));

        when(cacheRepository.findBySearchQuery("tesla"))
                .thenReturn(Optional.of(expiredCache));
        when(scraper.searchCompanies("tesla"))
                .thenReturn(scrapedCompanies);
        when(jsonMapper.writeValueAsString(scrapedCompanies))
                .thenReturn(CACHED_JSON);

        assertSame(scrapedCompanies, service.search("tesla", false));

        verify(scraper).searchCompanies("tesla");
        verify(cacheRepository).save(expiredCache);
    }

    @Test
    void forceRefreshBypassesFreshCache() throws Exception {
        SearchCache freshCache = new SearchCache(
                "tesla",
                CACHED_JSON,
                Instant.now().minus(1, ChronoUnit.HOURS)
        );
        List<Company> scrapedCompanies = List.of(company("01234567"));

        when(cacheRepository.findBySearchQuery("tesla"))
                .thenReturn(Optional.of(freshCache));
        when(scraper.searchCompanies("tesla"))
                .thenReturn(scrapedCompanies);
        when(jsonMapper.writeValueAsString(scrapedCompanies))
                .thenReturn(CACHED_JSON);

        assertSame(scrapedCompanies, service.search("tesla", true));

        verify(scraper).searchCompanies("tesla");
        verify(cacheRepository).save(freshCache);
        verify(jsonMapper, never()).readValue(
                eq(CACHED_JSON),
                any(TypeReference.class)
        );
    }

    @Test
    void blankQueryReturnsNoResultsWithoutCallingDependencies() {
        assertEquals(List.of(), service.search("   ", false));
        verifyNoInteractions(scraper, cacheRepository, jsonMapper);
    }

    private Company company(String companyNumber) {
        Company company = new Company();
        company.setCompanyNumber(companyNumber);
        return company;
    }
}
