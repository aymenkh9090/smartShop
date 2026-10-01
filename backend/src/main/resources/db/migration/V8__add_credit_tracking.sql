-- Marque permanente des ventes / achats a credit (non soldes a leur creation).
-- Elle reste vraie apres reglement complet : l'historique des credits est conserve.
alter table sale add column on_credit boolean not null default false;
alter table purchase add column on_credit boolean not null default false;

update sale set on_credit = true where amount_paid < total;
update purchase set on_credit = true where amount_paid < total;

-- Credits en cours (non soldes) et echeances
create index idx_sale_credit_open on sale (customer_id, due_date) where on_credit and payment_status <> 'PAID';
create index idx_purchase_credit_open on purchase (supplier_id, due_date) where on_credit and payment_status <> 'PAID';
