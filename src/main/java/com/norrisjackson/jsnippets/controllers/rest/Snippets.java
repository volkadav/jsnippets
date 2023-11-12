package com.norrisjackson.jsnippets.controllers.rest;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.SnippetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/snippets")
public class Snippets {
    @Autowired
    SnippetRepository snippets;

    @GetMapping
    public Iterable<Snippet> findAll() {
        return snippets.findAll(Sort.by(Sort.Direction.DESC,"createdAt"));
    }
}
