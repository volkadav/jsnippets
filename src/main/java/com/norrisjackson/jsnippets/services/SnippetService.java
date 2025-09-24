package com.norrisjackson.jsnippets.services;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.SnippetRepository;
import com.norrisjackson.jsnippets.data.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SnippetService {
    private final SnippetRepository snippetRepository;

    public SnippetService(SnippetRepository snippetRepository) {
        this.snippetRepository = snippetRepository;
    }

    public Snippet createSnippet(String snippetContent, User poster) {
        Snippet snippet = new Snippet();
        snippet.setContents(snippetContent);
        snippet.setPoster(poster);

        Date now = new Date();
        snippet.setCreatedAt(now);
        snippet.setEditedAt(now);

        return snippetRepository.save(snippet);
    }

    public Optional<Snippet> getSnippetById(Long id) {
        return snippetRepository.findById(id);
    }

    public List<Snippet> getAllSnippets() {
        return snippetRepository.findAll();
    }

    public Page<Snippet> getAllSnippets(Pageable pageable) {
        return snippetRepository.findAll(pageable);
    }

    public List<Snippet> getSnippetsByPosterId(Long posterId) {
        return snippetRepository.findByPosterId(posterId);
    }

    public Page<Snippet> getSnippetsByPosterId(Long posterId, Pageable pageable) {
        return snippetRepository.findByPosterId(posterId, pageable);
    }

    public Optional<Snippet> updateSnippet(Long id, String updatedSnippetContents) {
        return snippetRepository.findById(id).map(existingSnippet -> {
            existingSnippet.setContents(updatedSnippetContents);

            Date now = new Date();
            existingSnippet.setEditedAt(now);

            return snippetRepository.save(existingSnippet);
        });
    }

    public boolean deleteSnippet(Long id) {
        if (snippetRepository.existsById(id)) {
            snippetRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
