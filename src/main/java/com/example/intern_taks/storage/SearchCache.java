package com.example.intern_taks.storage;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "search_cache",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "search_query")
        }
)
public class SearchCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "search_query", nullable = false, unique = true)
    private String searchQuery;

    @Lob
    @Column(name = "results_json", nullable = false, columnDefinition = "CLOB")
    private String resultsJson;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    public SearchCache() {
    }

    public SearchCache(
            String searchQuery,
            String resultsJson,
            Instant fetchedAt
    ) {
        this.searchQuery = searchQuery;
        this.resultsJson = resultsJson;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public String getResultsJson() {
        return resultsJson;
    }

    public void setResultsJson(String resultsJson) {
        this.resultsJson = resultsJson;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}