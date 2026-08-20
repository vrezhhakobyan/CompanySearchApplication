package com.example.intern_taks.service;

import com.example.intern_taks.domain.Company;
import com.example.intern_taks.repository.SearchCacheRepository;
import com.example.intern_taks.scraper.CompaniesHouseScraper;
import com.example.intern_taks.storage.SearchCache;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class CompanySearchService {

    private static final Duration CACHE_TTL =
            Duration.ofHours(24);

    private final CompaniesHouseScraper scraper;
    private final SearchCacheRepository cacheRepository;
    private final JsonMapper jsonMapper;

    public CompanySearchService(
            CompaniesHouseScraper scraper,
            SearchCacheRepository cacheRepository,
            JsonMapper jsonMapper
    ) {
        this.scraper = scraper;
        this.cacheRepository = cacheRepository;
        this.jsonMapper = jsonMapper;
    }

    public List<Company> search(
            String query,
            boolean forceRefresh
    ) {
        String normalizedQuery = normalizeQuery(query);

        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        if (!forceRefresh) {
            SearchCache cached = cacheRepository
                    .findBySearchQuery(normalizedQuery)
                    .orElse(null);

            if (cached != null && isFresh(cached.getFetchedAt())) {
                return deserialize(cached.getResultsJson());
            }
        }

        List<Company> companies =
                scraper.searchCompanies(normalizedQuery);

        saveToCache(normalizedQuery, companies);

        return companies;
    }

    private boolean isFresh(Instant fetchedAt) {
        if (fetchedAt == null) {
            return false;
        }

        Duration age = Duration.between(
                fetchedAt,
                Instant.now()
        );

        return age.compareTo(CACHE_TTL) < 0;
    }

    private void saveToCache(
            String query,
            List<Company> companies
    ) {
        try {
            String json =
                    jsonMapper.writeValueAsString(companies);

            SearchCache cache = cacheRepository
                    .findBySearchQuery(query)
                    .orElse(new SearchCache());

            cache.setSearchQuery(query);
            cache.setResultsJson(json);
            cache.setFetchedAt(Instant.now());

            cacheRepository.save(cache);

        } catch (JacksonException exception) {
            throw new RuntimeException(
                    "Failed to serialize search results",
                    exception
            );
        }
    }

    private List<Company> deserialize(String json) {
        try {
            return jsonMapper.readValue(
                    json,
                    new TypeReference<List<Company>>() {
                    }
            );

        } catch (JacksonException exception) {
            throw new RuntimeException(
                    "Failed to deserialize cached results",
                    exception
            );
        }
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }

        return query
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }
}