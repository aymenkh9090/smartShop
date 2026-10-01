create sequence sale_reference_seq;

create table sale
(
    id             bigint generated always as identity primary key,
    reference      varchar(30)    not null,
    customer_id    bigint,
    status         varchar(20)    not null,
    subtotal       numeric(12, 3) not null,
    discount       numeric(12, 3) not null default 0,
    total          numeric(12, 3) not null,
    amount_paid    numeric(12, 3) not null,
    payment_status varchar(20)    not null,
    -- Echeance d'une vente a credit (reste du > 0)
    due_date       date,
    notes          varchar(1000),
    created_by     bigint         not null,
    created_at     timestamptz    not null,
    cancelled_by   bigint,
    cancelled_at   timestamptz,
    cancel_reason  varchar(255),
    constraint uk_sale_reference unique (reference),
    constraint fk_sale_customer foreign key (customer_id) references customer (id),
    constraint fk_sale_created_by foreign key (created_by) references app_user (id),
    constraint fk_sale_cancelled_by foreign key (cancelled_by) references app_user (id),
    constraint ck_sale_status check (status in ('COMPLETED', 'CANCELLED')),
    constraint ck_sale_payment_status check (payment_status in ('PAID', 'PARTIALLY_PAID', 'UNPAID')),
    constraint ck_sale_amounts check (subtotal >= 0 and discount >= 0 and discount <= subtotal
        and total = subtotal - discount and amount_paid >= 0 and amount_paid <= total),
    -- Une vente a credit doit etre rattachee a un client
    constraint ck_sale_credit_customer check (amount_paid = total or customer_id is not null)
);

create index idx_sale_customer_id on sale (customer_id);
create index idx_sale_created_at on sale (created_at desc);

create table sale_item
(
    id              bigint generated always as identity primary key,
    sale_id         bigint         not null,
    product_id      bigint         not null,
    quantity        integer        not null,
    -- Prix de vente copie au moment de la vente
    unit_price      numeric(12, 3) not null,
    line_total      numeric(12, 3) not null,
    warranty_months integer,
    constraint fk_sale_item_sale foreign key (sale_id) references sale (id),
    constraint fk_sale_item_product foreign key (product_id) references product (id),
    constraint uk_sale_item_product unique (sale_id, product_id),
    constraint ck_sale_item_values check (quantity > 0 and unit_price >= 0 and line_total = quantity * unit_price)
);

create index idx_sale_item_product_id on sale_item (product_id);

create table sale_item_unit
(
    sale_item_id bigint not null,
    unit_id      bigint not null,
    primary key (sale_item_id, unit_id),
    constraint fk_siu_item foreign key (sale_item_id) references sale_item (id),
    constraint fk_siu_unit foreign key (unit_id) references product_unit (id)
);

create index idx_siu_unit on sale_item_unit (unit_id);

create table sale_payment
(
    id         bigint generated always as identity primary key,
    sale_id    bigint         not null,
    amount     numeric(12, 3) not null,
    method     varchar(20)    not null,
    reference  varchar(100),
    paid_at    timestamptz    not null,
    created_by bigint         not null,
    constraint fk_sale_payment_sale foreign key (sale_id) references sale (id),
    constraint fk_sale_payment_user foreign key (created_by) references app_user (id),
    constraint ck_sale_payment_amount check (amount > 0),
    constraint ck_sale_payment_method check (method in ('CASH', 'CARD', 'BANK_TRANSFER', 'CHECK', 'OTHER'))
);

create index idx_sale_payment_sale_id on sale_payment (sale_id);
