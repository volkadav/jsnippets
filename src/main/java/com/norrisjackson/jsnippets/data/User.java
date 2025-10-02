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
    private String username;

    @NotNull
    private String email;

    @Column(name = "password_hash")
    @JsonIgnore
    private String passwordHash;

    @NotNull
    @JsonIgnore
    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "last_login")
    @JsonIgnore
    private Date lastLoggedInAt;

    @Column(name = "timezone")
    private String timezone = "UTC"; // Default to UTC, stores IANA timezone identifier
}
