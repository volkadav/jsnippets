package com.norrisjackson.jsnippets.services;

import com.norrisjackson.jsnippets.data.Snippet;
import com.norrisjackson.jsnippets.data.SnippetRepository;
import com.norrisjackson.jsnippets.data.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SnippetServiceTest {

    @Mock
    private SnippetRepository snippetRepository;

    @InjectMocks
    private SnippetService snippetService;

    private User testUser;
    private User anotherUser;
    private Snippet testSnippet;
    private Snippet anotherSnippet;
    private final String snippetContent = "System.out.println(\"Hello World\");";
    private final String updatedContent = "System.out.println(\"Hello Updated World\");";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");

        testSnippet = new Snippet();
        testSnippet.setId(1L);
        testSnippet.setContents(snippetContent);
        testSnippet.setPoster(testUser);
        testSnippet.setCreatedAt(new Date());
        testSnippet.setEditedAt(new Date());

        anotherSnippet = new Snippet();
        anotherSnippet.setId(2L);
        anotherSnippet.setContents("console.log('Hello from another user');");
        anotherSnippet.setPoster(anotherUser);
        anotherSnippet.setCreatedAt(new Date());
        anotherSnippet.setEditedAt(new Date());
    }

    @Test
    void createSnippet_WhenValidData_ReturnsCreatedSnippet() {
        // Given
        Snippet savedSnippet = new Snippet();
        savedSnippet.setId(1L);
        savedSnippet.setContents(snippetContent);
        savedSnippet.setPoster(testUser);
        savedSnippet.setCreatedAt(new Date());
        savedSnippet.setEditedAt(new Date());

        when(snippetRepository.save(any(Snippet.class))).thenReturn(savedSnippet);

        // When
        Snippet result = snippetService.createSnippet(snippetContent, testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContents()).isEqualTo(snippetContent);
        assertThat(result.getPoster()).isEqualTo(testUser);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getEditedAt()).isNotNull();

        verify(snippetRepository).save(any(Snippet.class));
    }

    @Test
    void getSnippetById_WhenSnippetExists_ReturnsSnippet() {
        // Given
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(testSnippet));

        // When
        Optional<Snippet> result = snippetService.getSnippetById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testSnippet);
        verify(snippetRepository).findById(1L);
    }

    @Test
    void getSnippetById_WhenSnippetDoesNotExist_ReturnsEmpty() {
        // Given
        when(snippetRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<Snippet> result = snippetService.getSnippetById(1L);

        // Then
        assertThat(result).isEmpty();
        verify(snippetRepository).findById(1L);
    }

    @Test
    void getAllSnippets_ReturnsAllSnippets() {
        // Given
        List<Snippet> snippets = Arrays.asList(testSnippet, anotherSnippet);
        when(snippetRepository.findAll()).thenReturn(snippets);

        // When
        List<Snippet> result = snippetService.getAllSnippets();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testSnippet, anotherSnippet);
        verify(snippetRepository).findAll();
    }

    @Test
    void getAllSnippets_WithPageable_ReturnsPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Snippet> snippets = Arrays.asList(testSnippet, anotherSnippet);
        Page<Snippet> page = new PageImpl<>(snippets, pageable, 2);
        when(snippetRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<Snippet> result = snippetService.getAllSnippets(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(testSnippet, anotherSnippet);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(snippetRepository).findAll(pageable);
    }

    @Test
    void getSnippetsByPosterId_ReturnsUserSnippets() {
        // Given
        List<Snippet> userSnippets = Arrays.asList(testSnippet);
        when(snippetRepository.findByPosterId(1L)).thenReturn(userSnippets);

        // When
        List<Snippet> result = snippetService.getSnippetsByPosterId(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSnippet);
        verify(snippetRepository).findByPosterId(1L);
    }

    @Test
    void getSnippetCountByPosterId_ReturnsCount() {
        // Given
        when(snippetRepository.countByPosterId(1L)).thenReturn(5L);

        // When
        long result = snippetService.getSnippetCountByPosterId(1L);

        // Then
        assertThat(result).isEqualTo(5L);
        verify(snippetRepository).countByPosterId(1L);
    }

    @Test
    void getSnippetsByPosterId_WithSort_ReturnsSortedUserSnippets() {
        // Given
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        List<Snippet> userSnippets = Arrays.asList(testSnippet);
        when(snippetRepository.findByPosterId(1L, sort)).thenReturn(userSnippets);

        // When
        List<Snippet> result = snippetService.getSnippetsByPosterId(1L, sort);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(testSnippet);
        verify(snippetRepository).findByPosterId(1L, sort);
    }

    @Test
    void getSnippetsByPosterId_WithPageable_ReturnsPagedUserSnippets() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Snippet> userSnippets = Arrays.asList(testSnippet);
        Page<Snippet> page = new PageImpl<>(userSnippets, pageable, 1);
        when(snippetRepository.findByPosterId(1L, pageable)).thenReturn(page);

        // When
        Page<Snippet> result = snippetService.getSnippetsByPosterId(1L, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).containsExactly(testSnippet);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(snippetRepository).findByPosterId(1L, pageable);
    }

    @Test
    void updateSnippet_WhenValidOwnerAndContent_UpdatesSnippet() {
        // Given
        Snippet existingSnippet = new Snippet();
        existingSnippet.setId(1L);
        existingSnippet.setContents(snippetContent);
        existingSnippet.setPoster(testUser);
        existingSnippet.setCreatedAt(new Date());
        existingSnippet.setEditedAt(new Date());

        Snippet updatedSnippet = new Snippet();
        updatedSnippet.setId(1L);
        updatedSnippet.setContents(updatedContent);
        updatedSnippet.setPoster(testUser);
        updatedSnippet.setCreatedAt(existingSnippet.getCreatedAt());
        updatedSnippet.setEditedAt(new Date());

        when(snippetRepository.findById(1L)).thenReturn(Optional.of(existingSnippet));
        when(snippetRepository.save(any(Snippet.class))).thenReturn(updatedSnippet);

        // When
        Optional<Snippet> result = snippetService.updateSnippet(1L, updatedContent, testUser);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getContents()).isEqualTo(updatedContent);
        verify(snippetRepository, times(2)).findById(1L); // Called twice: once for ownership check, once for update
        verify(snippetRepository).save(any(Snippet.class));
    }

    @Test
    void updateSnippet_WhenEmptyContent_ReturnsEmpty() {
        // When
        Optional<Snippet> result = snippetService.updateSnippet(1L, "", testUser);

        // Then
        assertThat(result).isEmpty();
        verify(snippetRepository, never()).findById(anyLong());
        verify(snippetRepository, never()).save(any(Snippet.class));
    }

    @Test
    void updateSnippet_WhenNullContent_ReturnsEmpty() {
        // When
        Optional<Snippet> result = snippetService.updateSnippet(1L, null, testUser);

        // Then
        assertThat(result).isEmpty();
        verify(snippetRepository, never()).findById(anyLong());
        verify(snippetRepository, never()).save(any(Snippet.class));
    }

    @Test
    void updateSnippet_WhenUserDoesNotOwnSnippet_ReturnsEmpty() {
        // Given
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(testSnippet));

        // When - anotherUser tries to update testSnippet owned by testUser
        Optional<Snippet> result = snippetService.updateSnippet(1L, updatedContent, anotherUser);

        // Then
        assertThat(result).isEmpty();
        verify(snippetRepository).findById(1L); // Called once in userOwnsSnippet check
        verify(snippetRepository, never()).save(any(Snippet.class));
    }

    @Test
    void updateSnippet_WhenSnippetDoesNotExist_ReturnsEmpty() {
        // Given
        when(snippetRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<Snippet> result = snippetService.updateSnippet(1L, updatedContent, testUser);

        // Then
        assertThat(result).isEmpty();
        verify(snippetRepository).findById(1L); // Called once for ownership check in updateSnippet
        verify(snippetRepository, never()).save(any(Snippet.class));
    }

    @Test
    void deleteSnippet_WhenUserOwnsSnippet_ReturnsTrue() {
        // Given
        when(snippetRepository.existsById(1L)).thenReturn(true);
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(testSnippet));

        // When
        boolean result = snippetService.deleteSnippet(1L, testUser);

        // Then
        assertThat(result).isTrue();
        verify(snippetRepository).existsById(1L);
        verify(snippetRepository).findById(1L);
        verify(snippetRepository).deleteById(1L);
    }

    @Test
    void deleteSnippet_WhenUserDoesNotOwnSnippet_ReturnsFalse() {
        // Given
        when(snippetRepository.existsById(1L)).thenReturn(true);
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(testSnippet));

        // When - anotherUser tries to delete testSnippet owned by testUser
        boolean result = snippetService.deleteSnippet(1L, anotherUser);

        // Then
        assertThat(result).isFalse();
        verify(snippetRepository).existsById(1L);
        verify(snippetRepository).findById(1L);
        verify(snippetRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteSnippet_WhenSnippetDoesNotExist_ReturnsFalse() {
        // Given
        when(snippetRepository.existsById(1L)).thenReturn(false);

        // When
        boolean result = snippetService.deleteSnippet(1L, testUser);

        // Then
        assertThat(result).isFalse();
        verify(snippetRepository).existsById(1L);
        verify(snippetRepository, never()).findById(anyLong());
        verify(snippetRepository, never()).deleteById(anyLong());
    }

    @Test
    void userOwnsSnippet_WhenUserOwnsSnippet_ReturnsTrue() {
        // Given
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(testSnippet));

        // When
        boolean result = snippetService.userOwnsSnippet(1L, 1L);

        // Then
        assertThat(result).isTrue();
        verify(snippetRepository).findById(1L);
    }

    @Test
    void userOwnsSnippet_WhenUserDoesNotOwnSnippet_ReturnsFalse() {
        // Given
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(testSnippet));

        // When
        boolean result = snippetService.userOwnsSnippet(1L, 2L);

        // Then
        assertThat(result).isFalse();
        verify(snippetRepository).findById(1L);
    }

    @Test
    void userOwnsSnippet_WhenSnippetDoesNotExist_ReturnsFalse() {
        // Given
        when(snippetRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        boolean result = snippetService.userOwnsSnippet(1L, 1L);

        // Then
        assertThat(result).isFalse();
        verify(snippetRepository).findById(1L);
    }

    @Test
    void retrieveSnippetForUser_WhenUserOwnsSnippet_ReturnsSnippet() {
        // Given
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(testSnippet));

        // When
        Optional<Snippet> result = snippetService.retrieveSnippetForUser(1L, 1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testSnippet);
        verify(snippetRepository, times(2)).findById(1L); // Called twice: once for exists check, once for ownership check
    }

    @Test
    void retrieveSnippetForUser_WhenUserDoesNotOwnSnippet_ReturnsEmpty() {
        // Given
        when(snippetRepository.findById(1L)).thenReturn(Optional.of(testSnippet));

        // When
        Optional<Snippet> result = snippetService.retrieveSnippetForUser(1L, 2L);

        // Then
        assertThat(result).isEmpty();
        verify(snippetRepository, times(2)).findById(1L); // Called twice: once for exists check, once for ownership check
    }

    @Test
    void retrieveSnippetForUser_WhenSnippetDoesNotExist_ReturnsEmpty() {
        // Given
        when(snippetRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<Snippet> result = snippetService.retrieveSnippetForUser(1L, 1L);

        // Then
        assertThat(result).isEmpty();
        verify(snippetRepository).findById(1L);
    }
}