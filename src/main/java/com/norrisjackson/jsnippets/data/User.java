package com.norrisjackson.jsnippets.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;

@Entity
@Table(name="users")
@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private String email;

    @Column(name = "salt")
    @JsonIgnore
    private String passwordSalt;

    @Column(name = "hash")
    @JsonIgnore
    private String passwordHash;

    @NotNull
    @JsonIgnore
    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "last_login")
    @JsonIgnore
    private Date lastLoggedInAt;
}
