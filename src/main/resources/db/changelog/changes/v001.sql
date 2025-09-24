create table users(
    id bigserial not null,
    username text not null,
    email text not null,
    password_hash text not null,
    created_at timestamp not null default now(),
    last_login timestamp,
    primary key (id)
);

create table snippets(
    id bigserial not null,
    contents text not null,
    poster_id bigint not null references users(id),
    created_at timestamp not null default now(),
    edited_at timestamp,
    primary key (id)
);