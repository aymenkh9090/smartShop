-- SKU genere pour les produits crees sans SKU (PRD-000001...)
create sequence product_sku_seq;

-- Credits saisis a la main (dette d'un client / envers un fournisseur sans vente ni achat enregistre).
-- Le reste du est toujours recalcule a partir des paiements (amount_paid = somme des paiements).
create sequence customer_credit_reference_seq;

create table customer_credit
(
    id             bigint generated always as identity primary key,
    reference      varchar(30)    not null,
    customer_id    bigint         not null,
    description    varchar(255),
    amount         numeric(12, 3) not null,
    amount_paid    numeric(12, 3) not null default 0,
    payment_status varchar(20)    not null,
    -- Date a laquelle la dette est nee (saisie par l'utilisateur, eventuellement passee)
    credited_at    timestamptz    not null,
    due_date       date,
    created_by     bigint         not null,
    created_at     timestamptz    not null,
    cancelled_by   bigint,
    cancelled_at   timestamptz,
    cancel_reason  varchar(255),
    constraint uk_customer_credit_reference unique (reference),
    constraint fk_customer_credit_customer foreign key (customer_id) references customer (id),
    constraint fk_customer_credit_created_by foreign key (created_by) references app_user (id),
    constraint fk_customer_credit_cancelled_by foreign key (cancelled_by) references app_user (id),
    constraint ck_customer_credit_payment_status check (payment_status in ('PAID', 'PARTIALLY_PAID', 'UNPAID')),
    constraint ck_customer_credit_amounts check (amount > 0 and amount_paid >= 0 and amount_paid <= amount)
);

create index idx_customer_credit_customer on customer_credit (customer_id, due_date);
create index idx_customer_credit_credited_at on customer_credit (credited_at desc);

create table customer_credit_payment
(
    id          bigint generated always as identity primary key,
    credit_id   bigint         not null,
    amount      numeric(12, 3) not null,
    method      varchar(20)    not null,
    reference   varchar(100),
    -- Date du versement declaree par l'utilisateur ; recorded_at = date de saisie
    paid_at     timestamptz    not null,
    recorded_at timestamptz    not null,
    created_by  bigint         not null,
    constraint fk_ccp_credit foreign key (credit_id) references customer_credit (id),
    constraint fk_ccp_user foreign key (created_by) references app_user (id),
    constraint ck_ccp_amount check (amount > 0),
    constraint ck_ccp_method check (method in ('CASH', 'CARD', 'BANK_TRANSFER', 'CHECK', 'OTHER'))
);

create index idx_ccp_credit on customer_credit_payment (credit_id);

create sequence supplier_credit_reference_seq;

create table supplier_credit
(
    id             bigint generated always as identity primary key,
    reference      varchar(30)    not null,
    supplier_id    bigint         not null,
    description    varchar(255),
    amount         numeric(12, 3) not null,
    amount_paid    numeric(12, 3) not null default 0,
    payment_status varchar(20)    not null,
    credited_at    timestamptz    not null,
    due_date       date,
    created_by     bigint         not null,
    created_at     timestamptz    not null,
    cancelled_by   bigint,
    cancelled_at   timestamptz,
    cancel_reason  varchar(255),
    constraint uk_supplier_credit_reference unique (reference),
    constraint fk_supplier_credit_supplier foreign key (supplier_id) references supplier (id),
    constraint fk_supplier_credit_created_by foreign key (created_by) references app_user (id),
    constraint fk_supplier_credit_cancelled_by foreign key (cancelled_by) references app_user (id),
    constraint ck_supplier_credit_payment_status check (payment_status in ('PAID', 'PARTIALLY_PAID', 'UNPAID')),
    constraint ck_supplier_credit_amounts check (amount > 0 and amount_paid >= 0 and amount_paid <= amount)
);

create index idx_supplier_credit_supplier on supplier_credit (supplier_id, due_date);
create index idx_supplier_credit_credited_at on supplier_credit (credited_at desc);

create table supplier_credit_payment
(
    id          bigint generated always as identity primary key,
    credit_id   bigint         not null,
    amount      numeric(12, 3) not null,
    method      varchar(20)    not null,
    reference   varchar(100),
    paid_at     timestamptz    not null,
    recorded_at timestamptz    not null,
    created_by  bigint         not null,
    constraint fk_scp_credit foreign key (credit_id) references supplier_credit (id),
    constraint fk_scp_user foreign key (created_by) references app_user (id),
    constraint ck_scp_amount check (amount > 0),
    constraint ck_scp_method check (method in ('CASH', 'CARD', 'BANK_TRANSFER', 'CHECK', 'OTHER'))
);

create index idx_scp_credit on supplier_credit_payment (credit_id);
