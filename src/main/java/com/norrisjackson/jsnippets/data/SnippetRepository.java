package com.norrisjackson.jsnippets.data;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SnippetRepository extends JpaRepository<Snippet, Integer> {

}
