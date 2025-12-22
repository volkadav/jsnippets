create table users(
    id bigserial not null,
    username text not null,
    email text not null,
    password_hash text not null,
    created_at timestamp not null default now(),
    last_login timestamp,
    primary key (id)
);

-- Indexes for users table
create unique index idx_users_username on users(username);
create unique index idx_users_email on users(email);
create index idx_users_created_at on users(created_at desc);

create table snippets(
    id bigserial not null,
    contents text not null,
    poster_id bigint not null references users(id),
    created_at timestamp not null default now(),
    edited_at timestamp,
    primary key (id)
);

-- Indexes for snippets table
create index idx_snippets_poster_id on snippets(poster_id);
create index idx_snippets_created_at on snippets(created_at desc);
create index idx_snippets_edited_at on snippets(edited_at desc);
create index idx_snippets_poster_edited on snippets(poster_id, edited_at desc);
