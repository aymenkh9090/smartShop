-- Unites suivies individuellement (IMEI / numero de serie) des produits serial_tracked
create table product_unit
(
    id            bigint generated always as identity primary key,
    product_id    bigint      not null,
    imei          varchar(15),
    serial_number varchar(50),
    status        varchar(20) not null,
    created_at    timestamptz not null,
    updated_at    timestamptz not null,
    constraint fk_product_unit_product foreign key (product_id) references product (id),
    constraint uk_product_unit_imei unique (imei),
    constraint uk_product_unit_serial unique (product_id, serial_number),
    constraint ck_product_unit_identifier check (imei is not null or serial_number is not null),
    constraint ck_product_unit_status check (status in ('IN_STOCK', 'SOLD', 'REMOVED'))
);

create index idx_product_unit_product_status on product_unit (product_id, status);

-- Journal immuable des mouvements de stock : seule source de modification de product.quantity
create table stock_movement
(
    id              bigint generated always as identity primary key,
    product_id      bigint       not null,
    type            varchar(20)  not null,
    -- Variation signee (+ entree, - sortie) et quantite resultante, pour un historique lisible sans recalcul
    quantity_change integer      not null,
    quantity_after  integer      not null,
    reason          varchar(255),
    reference       varchar(100),
    created_by      bigint       not null,
    created_at      timestamptz  not null,
    constraint fk_stock_movement_product foreign key (product_id) references product (id),
    constraint fk_stock_movement_user foreign key (created_by) references app_user (id),
    constraint ck_stock_movement_type check (type in ('STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT', 'RETURN')),
    constraint ck_stock_movement_change check (quantity_change <> 0),
    constraint ck_stock_movement_after check (quantity_after >= 0)
);

create index idx_stock_movement_product_date on stock_movement (product_id, created_at desc);
create index idx_stock_movement_created_at on stock_movement (created_at desc);
create index idx_stock_movement_reference on stock_movement (reference);

-- Unites concernees par chaque mouvement (tracabilite IMEI)
create table stock_movement_unit
(
    movement_id bigint not null,
    unit_id     bigint not null,
    primary key (movement_id, unit_id),
    constraint fk_smu_movement foreign key (movement_id) references stock_movement (id),
    constraint fk_smu_unit foreign key (unit_id) references product_unit (id)
);

create index idx_smu_unit on stock_movement_unit (unit_id);
