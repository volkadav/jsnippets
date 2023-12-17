package com.norrisjackson.jsnippets.data;

import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "snippets")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class Snippet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String contents;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "poster_id")
    private User poster;

    @Column(name = "created_at")
    @NotNull
    private Date createdAt;

    @Column(name = "edited_at")
    private Date editedAt;
}
