package com.norrisjackson.jsnippets.controllers.rest;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.SnippetRepository;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Date;
import java.util.Optional;


@RestController
@RequestMapping(path = "/api/snippets", produces = "application/json")
@Slf4j
public class Snippets {
    @Autowired
    SnippetRepository snippets;

    @Autowired
    UserRepository users;

    @GetMapping
    public Iterable<Snippet> findAll(@RequestParam("pageNumber") Optional<Integer> pageNumber,
                                     @RequestParam("pageSize") Optional<Integer> pageSize,
                                     @RequestParam("posterId") Optional<Long> posterId) {
        PageRequest page = PageRequest.of(pageNumber.orElse(0), pageSize.orElse(20),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        if (posterId.isPresent()) {
            User poster = users.findById(posterId.get()).orElse(null);
            if (poster == null) {
                return Collections.emptyList();
            }
            return snippets.findByPosterId(posterId.get(), page);
        } else {
            return snippets.findAll(page).getContent();
        }
    }

    @GetMapping("/{snippetId}")
    public ResponseEntity<Snippet> findById(@PathVariable("snippetId") Long snippetId) {
        Snippet s = snippets.findById(snippetId).orElse(null);

        return (s == null)
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
                : ResponseEntity.ok(s);
    }



    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Snippet> addSnippet(@RequestParam Long posterId,
                              @RequestParam String contents) {
        User poster = users.findById(posterId).orElse(null);
        if (poster == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Snippet s = new Snippet();
        s.setPoster(poster);
        s.setContents(contents);
        s.setCreatedAt(new Date());
        return ResponseEntity.ok(snippets.save(s));
    }

    @PatchMapping(path = "/{snippetId}", consumes = "application/json")
    public ResponseEntity<Snippet> editSnippet(@PathVariable("snippetId") Long snippetId,
                                      @RequestBody Snippet snippetNew) {
        Snippet snippetOld = snippets.findById(snippetId).orElse(null);

        if (snippetOld == null || Strings.isBlank(snippetNew.getContents())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        snippetOld.setContents(snippetNew.getContents());
        snippetOld.setEditedAt(new Date());
        snippets.save(snippetOld);

        return ResponseEntity.ok(snippetOld);
    }

    @DeleteMapping("/{snippetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSnippet(@PathVariable Long snippetId) {
        try {
            snippets.deleteById(snippetId);
        } catch (EmptyResultDataAccessException e) {
            log.info("someone tried to delete a snippet with id {} that doesn't exist", snippetId);
        }
    }
}
