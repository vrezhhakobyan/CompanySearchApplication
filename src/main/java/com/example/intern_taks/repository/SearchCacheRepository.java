package com.example.intern_taks.repository;

import com.example.intern_taks.storage.SearchCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SearchCacheRepository extends JpaRepository<SearchCache, Long> {

    Optional<SearchCache> findBySearchQuery(String searchQuery);
}