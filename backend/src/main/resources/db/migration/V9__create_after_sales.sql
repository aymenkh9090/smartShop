create sequence after_sales_ticket_number_seq;

-- Ticket SAV : appareil confie par un client, de la reception a la livraison
create table after_sales_ticket
(
    id                 bigint generated always as identity primary key,
    ticket_number      varchar(30)    not null,
    customer_id        bigint         not null,
    -- Appareil : description libre (appareil achete ailleurs) et, si connus, produit / unite / vente d'origine
    device_description varchar(200)   not null,
    product_id         bigint,
    unit_id            bigint,
    sale_id            bigint,
    imei               varchar(15),
    serial_number      varchar(50),
    reported_issue     varchar(1000)  not null,
    diagnosis          varchar(2000),
    repair_description varchar(2000),
    technician_id      bigint,
    status             varchar(20)    not null,
    -- Fin de garantie de la vente d'origine ; under_warranty = reparation prise en charge (cout facture nul)
    warranty_until     date,
    under_warranty     boolean        not null,
    estimated_cost     numeric(12, 3),
    labor_cost         numeric(12, 3) not null default 0,
    parts_total        numeric(12, 3) not null default 0,
    total_cost         numeric(12, 3) not null default 0,
    amount_paid        numeric(12, 3) not null default 0,
    payment_status     varchar(20)    not null,
    received_at        timestamptz    not null,
    expected_date      date,
    delivered_at       timestamptz,
    notes              varchar(1000),
    created_by         bigint         not null,
    created_at         timestamptz    not null,
    updated_at         timestamptz    not null,
    constraint uk_after_sales_ticket_number unique (ticket_number),
    constraint fk_ast_customer foreign key (customer_id) references customer (id),
    constraint fk_ast_product foreign key (product_id) references product (id),
    constraint fk_ast_unit foreign key (unit_id) references product_unit (id),
    constraint fk_ast_sale foreign key (sale_id) references sale (id),
    constraint fk_ast_technician foreign key (technician_id) references app_user (id),
    constraint fk_ast_created_by foreign key (created_by) references app_user (id),
    constraint ck_ast_status check (status in ('RECEIVED', 'DIAGNOSING', 'WAITING_FOR_PART', 'IN_REPAIR',
                                               'READY', 'DELIVERED', 'CANCELLED')),
    constraint ck_ast_payment_status check (payment_status in ('PAID', 'PARTIALLY_PAID', 'UNPAID')),
    constraint ck_ast_warranty check (not under_warranty or warranty_until is not null),
    constraint ck_ast_amounts check (labor_cost >= 0 and parts_total >= 0 and amount_paid >= 0
        and (estimated_cost is null or estimated_cost >= 0)
        and total_cost = case when under_warranty then 0 else labor_cost + parts_total end
        and amount_paid <= total_cost),
    -- Un ticket livre est solde
    constraint ck_ast_delivered check ((status = 'DELIVERED') = (delivered_at is not null)
        and (status <> 'DELIVERED' or amount_paid = total_cost))
);

create index idx_ast_customer_id on after_sales_ticket (customer_id);
create index idx_ast_technician_status on after_sales_ticket (technician_id, status);
create index idx_ast_status on after_sales_ticket (status);
create index idx_ast_received_at on after_sales_ticket (received_at desc);
create index idx_ast_imei on after_sales_ticket (imei);
create index idx_ast_serial_number on after_sales_ticket (serial_number);

-- Historique immuable des changements de statut (creation incluse : from_status null)
create table after_sales_status_change
(
    id          bigint generated always as identity primary key,
    ticket_id   bigint       not null,
    from_status varchar(20),
    to_status   varchar(20)  not null,
    comment     varchar(500),
    changed_by  bigint       not null,
    changed_at  timestamptz  not null,
    constraint fk_assc_ticket foreign key (ticket_id) references after_sales_ticket (id),
    constraint fk_assc_user foreign key (changed_by) references app_user (id)
);

create index idx_assc_ticket on after_sales_status_change (ticket_id, changed_at);

-- Pieces utilisees : chacune correspond a une sortie de stock (reference = numero de ticket)
create table after_sales_part
(
    id         bigint generated always as identity primary key,
    ticket_id  bigint         not null,
    product_id bigint         not null,
    quantity   integer        not null,
    -- Prix de vente copie au moment de l'utilisation
    unit_price numeric(12, 3) not null,
    line_total numeric(12, 3) not null,
    added_by   bigint         not null,
    added_at   timestamptz    not null,
    constraint fk_asp_ticket foreign key (ticket_id) references after_sales_ticket (id),
    constraint fk_asp_product foreign key (product_id) references product (id),
    constraint fk_asp_user foreign key (added_by) references app_user (id),
    constraint ck_asp_values check (quantity > 0 and unit_price >= 0 and line_total = quantity * unit_price)
);

create index idx_asp_ticket on after_sales_part (ticket_id);
create index idx_asp_product on after_sales_part (product_id);

create table after_sales_part_unit
(
    part_id bigint not null,
    unit_id bigint not null,
    primary key (part_id, unit_id),
    constraint fk_aspu_part foreign key (part_id) references after_sales_part (id),
    constraint fk_aspu_unit foreign key (unit_id) references product_unit (id)
);

create index idx_aspu_unit on after_sales_part_unit (unit_id);

create table after_sales_payment
(
    id         bigint generated always as identity primary key,
    ticket_id  bigint         not null,
    amount     numeric(12, 3) not null,
    method     varchar(20)    not null,
    reference  varchar(100),
    paid_at    timestamptz    not null,
    created_by bigint         not null,
    constraint fk_aspay_ticket foreign key (ticket_id) references after_sales_ticket (id),
    constraint fk_aspay_user foreign key (created_by) references app_user (id),
    constraint ck_aspay_amount check (amount > 0),
    constraint ck_aspay_method check (method in ('CASH', 'CARD', 'BANK_TRANSFER', 'CHECK', 'OTHER'))
);

create index idx_aspay_ticket on after_sales_payment (ticket_id);
