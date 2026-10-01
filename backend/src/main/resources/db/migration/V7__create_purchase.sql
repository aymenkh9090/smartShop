create sequence purchase_reference_seq;

create table purchase
(
    id                   bigint generated always as identity primary key,
    reference            varchar(30)    not null,
    supplier_id          bigint         not null,
    -- Numero de facture / bon de livraison du fournisseur
    supplier_invoice_ref varchar(100),
    total                numeric(12, 3) not null,
    amount_paid          numeric(12, 3) not null,
    payment_status       varchar(20)    not null,
    due_date             date,
    notes                varchar(1000),
    created_by           bigint         not null,
    created_at           timestamptz    not null,
    constraint uk_purchase_reference unique (reference),
    constraint fk_purchase_supplier foreign key (supplier_id) references supplier (id),
    constraint fk_purchase_created_by foreign key (created_by) references app_user (id),
    constraint ck_purchase_payment_status check (payment_status in ('PAID', 'PARTIALLY_PAID', 'UNPAID')),
    constraint ck_purchase_amounts check (total >= 0 and amount_paid >= 0 and amount_paid <= total)
);

create index idx_purchase_supplier_id on purchase (supplier_id);
create index idx_purchase_created_at on purchase (created_at desc);

create table purchase_item
(
    id          bigint generated always as identity primary key,
    purchase_id bigint         not null,
    product_id  bigint         not null,
    quantity    integer        not null,
    unit_cost   numeric(12, 3) not null,
    line_total  numeric(12, 3) not null,
    constraint fk_purchase_item_purchase foreign key (purchase_id) references purchase (id),
    constraint fk_purchase_item_product foreign key (product_id) references product (id),
    constraint uk_purchase_item_product unique (purchase_id, product_id),
    constraint ck_purchase_item_values check (quantity > 0 and unit_cost >= 0 and line_total = quantity * unit_cost)
);

create index idx_purchase_item_product_id on purchase_item (product_id);

create table purchase_item_unit
(
    purchase_item_id bigint not null,
    unit_id          bigint not null,
    primary key (purchase_item_id, unit_id),
    constraint fk_piu_item foreign key (purchase_item_id) references purchase_item (id),
    constraint fk_piu_unit foreign key (unit_id) references product_unit (id)
);

create index idx_piu_unit on purchase_item_unit (unit_id);

create table purchase_payment
(
    id          bigint generated always as identity primary key,
    purchase_id bigint         not null,
    amount      numeric(12, 3) not null,
    method      varchar(20)    not null,
    reference   varchar(100),
    paid_at     timestamptz    not null,
    created_by  bigint         not null,
    constraint fk_purchase_payment_purchase foreign key (purchase_id) references purchase (id),
    constraint fk_purchase_payment_user foreign key (created_by) references app_user (id),
    constraint ck_purchase_payment_amount check (amount > 0),
    constraint ck_purchase_payment_method check (method in ('CASH', 'CARD', 'BANK_TRANSFER', 'CHECK', 'OTHER'))
);

create index idx_purchase_payment_purchase_id on purchase_payment (purchase_id);
