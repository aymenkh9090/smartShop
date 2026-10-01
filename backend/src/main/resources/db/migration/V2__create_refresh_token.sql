-- Refresh tokens : seule l'empreinte SHA-256 est stockee, jamais la valeur brute.
create table refresh_token
(
    id         bigint generated always as identity primary key,
    user_id    bigint      not null,
    token_hash varchar(64) not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null,
    constraint uk_refresh_token_hash unique (token_hash),
    constraint fk_refresh_token_user foreign key (user_id) references app_user (id) on delete cascade
);

create index idx_refresh_token_user_id on refresh_token (user_id);
