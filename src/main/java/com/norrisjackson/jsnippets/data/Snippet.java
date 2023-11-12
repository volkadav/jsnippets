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
    private Integer id;

    @NotNull
    private String contents;

    @JoinColumn(name = "poster_id")
    @NotNull
    @ManyToOne
    private User poster;

    @Column(name = "created_at")
    @NotNull
    private Date createdAt;

    @Column(name = "edited_at")
    private Date editedAt;
}
