package com.norrisjackson.jsnippets.controllers.rest;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.SnippetRepository;
import com.norrisjackson.jsnippets.data.User;
import com.norrisjackson.jsnippets.data.UserRepository;
import com.norrisjackson.jsnippets.services.SnippetService;
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
public class SnippetsApi {
    @Autowired
    SnippetRepository snippets;

    @Autowired
    UserRepository users;

    @Autowired
    SnippetService snippetService;

    @GetMapping
    public Iterable<Snippet> findAll(@RequestParam("pageNumber") Optional<Integer> pageNumber,
                                     @RequestParam("pageSize") Optional<Integer> pageSize,
                                     @RequestParam("posterId") Optional<Long> posterId) {
        // Paging and sorting logic remains here, but delegate to service for fetching
        PageRequest page = PageRequest.of(pageNumber.orElse(0), pageSize.orElse(20),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        if (posterId.isPresent()) {
            User poster = users.findById(posterId.get()).orElse(null);
            if (poster == null) {
                return Collections.emptyList();
            }
            // If you want to move this logic to the service, add a method for it
            return snippetService.getSnippetsByPosterId(posterId.get(), page);
        } else {
            return snippetService.getAllSnippets(page);
        }
    }

    @GetMapping("/{snippetId}")
    public ResponseEntity<Snippet> findById(@PathVariable("snippetId") Long snippetId) {
        Optional<Snippet> s = snippetService.getSnippetById(snippetId);
        return s.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Snippet> addSnippet(@RequestParam Long posterId,
                                              @RequestParam String contents) {

        // todo: auth token or something to authenticate request
        User poster = users.findById(posterId).orElse(null);
        if (poster == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        return ResponseEntity.ok(snippetService.createSnippet(contents, poster));
    }

    @PatchMapping(path = "/{snippetId}", consumes = "application/json")
    public ResponseEntity<Snippet> editSnippet(@PathVariable("snippetId") Long snippetId,
                                      @RequestBody Snippet snippetNew) {
        if (Strings.isBlank(snippetNew.getContents())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        Optional<Snippet> updated = snippetService.updateSnippet(snippetId, snippetNew.getContents());
        return updated.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null));
    }

    @DeleteMapping("/{snippetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSnippet(@PathVariable Long snippetId) {
        try {
            snippetService.deleteSnippet(snippetId);
        } catch (EmptyResultDataAccessException e) {
            log.info("someone tried to delete a snippet with id {} that doesn't exist", snippetId);
        }
    }
}
