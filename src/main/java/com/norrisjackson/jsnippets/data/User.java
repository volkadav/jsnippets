package com.norrisjackson.jsnippets.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name="users")
@Getter
@Setter
@RequiredArgsConstructor
@ToString(exclude = {"followedUsers", "followers"})
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

    @ManyToMany
    @JoinTable(
        name = "followers",
        joinColumns = @JoinColumn(name = "follower_id"),
        inverseJoinColumns = @JoinColumn(name = "followed_id")
    )
    @JsonIgnore
    private List<User> followedUsers = new ArrayList<>(); // Initialize to prevent NullPointerException

    @ManyToMany(mappedBy = "followedUsers")
    @JsonIgnore
    private List<User> followers;
}
