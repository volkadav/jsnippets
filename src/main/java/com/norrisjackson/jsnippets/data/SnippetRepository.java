package com.norrisjackson.jsnippets.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SnippetRepository extends JpaRepository<Snippet, Long> {
    List<Snippet> findByPosterId(Long posterId);
    List<Snippet> findByPosterId(Long posterId, Sort sort);
    Page<Snippet> findByPosterId(Long posterId, Pageable pageable);
}

