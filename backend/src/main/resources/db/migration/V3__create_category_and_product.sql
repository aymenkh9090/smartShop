create table category
(
    id          bigint generated always as identity primary key,
    name        varchar(100) not null,
    description varchar(500),
    created_at  timestamptz  not null,
    updated_at  timestamptz  not null
);

-- Nom unique sans tenir compte de la casse
create unique index uk_category_name on category (lower(name));

create table product
(
    id                bigint generated always as identity primary key,
    sku               varchar(50)    not null,
    barcode           varchar(50),
    reference         varchar(100),
    name              varchar(150)   not null,
    description       varchar(2000),
    category_id       bigint         not null,
    type              varchar(30)    not null,
    product_condition varchar(20)    not null,
    brand             varchar(100),
    model             varchar(100),
    purchase_price    numeric(12, 3) not null,
    sale_price        numeric(12, 3) not null,
    -- Modifiee uniquement par les mouvements de stock (phase 4)
    quantity          integer        not null default 0,
    min_stock         integer        not null default 0,
    serial_tracked    boolean        not null default false,
    warranty_months   integer,
    active            boolean        not null default true,
    created_at        timestamptz    not null,
    updated_at        timestamptz    not null,
    constraint uk_product_sku unique (sku),
    constraint uk_product_barcode unique (barcode),
    constraint fk_product_category foreign key (category_id) references category (id),
    constraint ck_product_type check (type in ('SMARTPHONE', 'RECEIVER', 'IPTV_SUBSCRIPTION', 'CHARGER', 'CABLE',
                                               'EARPHONE', 'SMARTWATCH', 'ACCESSORY', 'OTHER')),
    constraint ck_product_condition check (product_condition in ('NEW', 'USED', 'REFURBISHED')),
    constraint ck_product_prices check (purchase_price >= 0 and sale_price >= 0),
    constraint ck_product_quantity check (quantity >= 0),
    constraint ck_product_min_stock check (min_stock >= 0),
    constraint ck_product_warranty check (warranty_months is null or warranty_months >= 0)
);

create index idx_product_category_id on product (category_id);
create index idx_product_name on product (lower(name));
