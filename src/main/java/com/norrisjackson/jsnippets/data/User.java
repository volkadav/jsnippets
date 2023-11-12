package com.norrisjackson.jsnippets.data;

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
    private Integer id;

    @NotNull
    private String name;

    @NotNull
    private String email;

    @Column(name = "salt")
    private String passwordSalt;

    @Column(name = "hash")
    private String passwordHash;

    @NotNull
    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "last_login")
    private Date lastLoggedInAt;
}
