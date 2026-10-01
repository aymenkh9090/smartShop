create table customer
(
    id         bigint generated always as identity primary key,
    first_name varchar(100)  not null,
    last_name  varchar(100)  not null,
    -- Cle de recherche au comptoir : un numero = un client
    phone      varchar(20)   not null,
    email      varchar(150),
    address    varchar(255),
    cin        varchar(20),
    notes      varchar(1000),
    active     boolean       not null default true,
    created_at timestamptz   not null,
    updated_at timestamptz   not null,
    constraint uk_customer_phone unique (phone),
    constraint uk_customer_cin unique (cin)
);

create index idx_customer_last_name on customer (lower(last_name));

create table supplier
(
    id         bigint generated always as identity primary key,
    name       varchar(150)  not null,
    phone      varchar(20),
    email      varchar(150),
    address    varchar(255),
    tax_id     varchar(30),
    notes      varchar(1000),
    active     boolean       not null default true,
    created_at timestamptz   not null,
    updated_at timestamptz   not null,
    constraint uk_supplier_tax_id unique (tax_id)
);

create unique index uk_supplier_name on supplier (lower(name));
