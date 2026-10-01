-- Utilisateurs de l'application (le personnel de la boutique).
-- "user" etant un mot reserve PostgreSQL, la table s'appelle app_user.
create table app_user
(
    id            bigint generated always as identity primary key,
    username      varchar(50)  not null,
    email         varchar(150) not null,
    password_hash varchar(100) not null,
    first_name    varchar(100) not null,
    last_name     varchar(100) not null,
    role          varchar(20)  not null,
    enabled       boolean      not null default true,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    constraint uk_app_user_username unique (username),
    constraint uk_app_user_email unique (email),
    constraint ck_app_user_role check (role in ('ADMIN', 'MANAGER', 'SELLER', 'TECHNICIAN'))
);
