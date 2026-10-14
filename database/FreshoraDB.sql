-- Freshora: run the entire file with F5 in normal SSMS. No SQLCMD mode needed.
-- Creates a new database or upgrades the existing database without resetting business records.
USE master;
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;

IF DB_ID(N'FreshoraDB') IS NOT NULL
BEGIN
    PRINT N'FreshoraDB already exists. Applying schema and catalog updates.';
    PRINT N'Use FreshoraDB to connect your application. Do not delete the database to rerun this file.';
END
ELSE
BEGIN
BEGIN TRY
    EXEC(N'CREATE DATABASE FreshoraDB COLLATE Latin1_General_100_CI_AS_SC');
    EXEC sys.sp_executesql N'USE FreshoraDB;
BEGIN TRANSACTION;
    EXEC sys.sp_executesql N''-- Freshora SQL Server 2019+. Fresh install only; never drops existing data.

SET NOCOUNT ON; SET XACT_ABORT ON;


CREATE TABLE dbo.[settings] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [type] VARCHAR(30) NOT NULL CHECK ([type] IN (''''CATEGORY'''', ''''SELLER_TIER'''', ''''MEMBERSHIP_TIER'''', ''''SERVICE_AREA'''', ''''POLICY'''')),
    [name] NVARCHAR(100) NOT NULL,
    [value] NVARCHAR(500) NULL,
    [parent_id] BIGINT NULL REFERENCES dbo.[settings]([id]),
    [priority] INT NOT NULL DEFAULT 0,
    [status] VARCHAR(20) NOT NULL DEFAULT ''''ACTIVE'''' CHECK ([status] IN (''''ACTIVE'''', ''''INACTIVE'''')),
    [description] NVARCHAR(255) NULL,
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[users] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [email] NVARCHAR(254) NOT NULL UNIQUE,
    [password_hash] VARCHAR(255) NOT NULL,
    [full_name] NVARCHAR(100) NOT NULL,
    [phone] VARCHAR(20) NULL,
    [role] VARCHAR(20) NOT NULL DEFAULT ''''CUSTOMER'''' CHECK ([role] IN (''''CUSTOMER'''', ''''SELLER'''', ''''DELIVERY_STAFF'''', ''''ADMIN'''')),
    [membership_tier_id] BIGINT NULL REFERENCES dbo.[settings]([id]),
    [points_balance] INT NOT NULL DEFAULT 0 CHECK (points_balance >= 0),
    [status] VARCHAR(20) NOT NULL DEFAULT ''''ACTIVE'''' CHECK ([status] IN (''''ACTIVE'''', ''''SUSPENDED'''')),
    [last_login_at] DATETIME2 NULL,
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[addresses] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [user_id] BIGINT NOT NULL REFERENCES dbo.[users]([id]),
    [recipient_name] NVARCHAR(100) NOT NULL,
    [phone] VARCHAR(20) NOT NULL,
    [address_line] NVARCHAR(255) NOT NULL,
    [ward] NVARCHAR(100) NOT NULL,
    [district] NVARCHAR(100) NOT NULL,
    [city] NVARCHAR(100) NOT NULL,
    [is_default] BIT NOT NULL DEFAULT 0,
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[stores] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [owner_id] BIGINT NOT NULL UNIQUE REFERENCES dbo.[users]([id]),
    [store_name] NVARCHAR(100) NOT NULL UNIQUE,
    [description] NVARCHAR(1000) NULL,
    [business_info] NVARCHAR(1000) NULL,
    [pickup_address_id] BIGINT NULL REFERENCES dbo.[addresses]([id]),
    [seller_tier_id] BIGINT NULL REFERENCES dbo.[settings]([id]),
    [rating] DECIMAL(3,2) NOT NULL DEFAULT 0,
    [status] VARCHAR(20) NOT NULL DEFAULT ''''ACTIVE'''' CHECK (status IN (''''ACTIVE'''',''''SUSPENDED'''',''''CLOSED'''')),
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[promotions] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [type] VARCHAR(20) NOT NULL CHECK ([type] IN (''''SELLER_CAMPAIGN'''', ''''VOUCHER'''')),
    [store_id] BIGINT NULL REFERENCES dbo.[stores]([id]),
    [name] NVARCHAR(150) NOT NULL,
    [code] VARCHAR(30) NULL,
    [discount_type] VARCHAR(10) NOT NULL CHECK ([discount_type] IN (''''PERCENT'''', ''''AMOUNT'''')),
    [discount_value] DECIMAL(15,2) NOT NULL CHECK (discount_value > 0),
    [max_discount] DECIMAL(15,2) NULL,
    [min_spend] DECIMAL(15,2) NOT NULL DEFAULT 0,
    [budget_limit] DECIMAL(15,2) NULL,
    [budget_used] DECIMAL(15,2) NOT NULL DEFAULT 0,
    [usage_limit] INT NULL,
    [per_customer_limit] INT NULL,
    [used_count] INT NOT NULL DEFAULT 0,
    [start_at] DATETIME2 NOT NULL,
    [end_at] DATETIME2 NOT NULL,
    [status] VARCHAR(20) NOT NULL DEFAULT ''''DRAFT'''' CHECK ([status] IN (''''DRAFT'''', ''''SCHEDULED'''', ''''RUNNING'''', ''''PAUSED'''', ''''ENDED'''', ''''EXHAUSTED'''', ''''DISABLED'''')),
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[listings] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [store_id] BIGINT NOT NULL REFERENCES dbo.[stores]([id]),
    [category_id] BIGINT NOT NULL REFERENCES dbo.[settings]([id]),
    [campaign_id] BIGINT NULL REFERENCES dbo.[promotions]([id]),
    [listing_type] VARCHAR(20) NOT NULL DEFAULT ''''PRODUCT'''' CHECK ([listing_type] IN (''''PRODUCT'''', ''''PLAN'''')),
    [title] NVARCHAR(200) NOT NULL,
    [description] NVARCHAR(MAX) NULL,
    [origin] NVARCHAR(100) NULL,
    [harvest_info] NVARCHAR(255) NULL,
    [image_urls] NVARCHAR(MAX) NULL,
    [plan_frequency] VARCHAR(20) NULL CHECK ([plan_frequency] IN (''''WEEKLY'''', ''''BIWEEKLY'''', ''''MONTHLY'''')),
    [plan_terms] NVARCHAR(1000) NULL,
    [enrollment_open] BIT NOT NULL DEFAULT 0,
    [rating] DECIMAL(3,2) NOT NULL DEFAULT 0,
    [status] VARCHAR(20) NOT NULL DEFAULT ''''DRAFT'''' CHECK ([status] IN (''''DRAFT'''', ''''PENDING_REVIEW'''', ''''APPROVED'''', ''''REJECTED'''', ''''HIDDEN'''', ''''RETIRED'''')),
    [rejection_reason] NVARCHAR(500) NULL,
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[variants] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [listing_id] BIGINT NOT NULL REFERENCES dbo.[listings]([id]),
    [option_name] NVARCHAR(100) NOT NULL,
    [unit] NVARCHAR(20) NOT NULL,
    [price] DECIMAL(15,2) NOT NULL CHECK (price > 0),
    [status] VARCHAR(20) NOT NULL DEFAULT ''''ACTIVE'''' CHECK ([status] IN (''''ACTIVE'''', ''''RETIRED'''')),
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[batches] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [variant_id] BIGINT NOT NULL REFERENCES dbo.[variants]([id]),
    [batch_no] VARCHAR(30) NOT NULL,
    [origin] NVARCHAR(100) NOT NULL,
    [harvest_date] DATE NOT NULL,
    [receipt_date] DATE NOT NULL,
    [expiry_date] DATE NOT NULL,
    [supplier_name] NVARCHAR(150) NOT NULL,
    [received_qty] INT NOT NULL CHECK (received_qty > 0),
    [remaining_qty] INT NOT NULL CHECK (remaining_qty >= 0),
    [reserved_qty] INT NOT NULL DEFAULT 0,
    [unit_cost] DECIMAL(15,2) NOT NULL CHECK (unit_cost >= 0),
    [total_cost] AS (CONVERT(decimal(19,2),received_qty * unit_cost)) PERSISTED,
    [status] VARCHAR(20) NOT NULL DEFAULT ''''AVAILABLE'''' CHECK ([status] IN (''''AVAILABLE'''', ''''DEPLETED'''', ''''EXPIRED'''')),
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[subscriptions] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [customer_id] BIGINT NOT NULL REFERENCES dbo.[users]([id]),
    [variant_id] BIGINT NOT NULL REFERENCES dbo.[variants]([id]),
    [quantity] INT NOT NULL DEFAULT 1 CHECK (quantity > 0),
    [delivery_address] NVARCHAR(500) NOT NULL,
    [payment_method] VARCHAR(20) NOT NULL CHECK ([payment_method] IN (''''COD'''', ''''CARD'''', ''''E_WALLET'''')),
    [start_date] DATE NOT NULL,
    [next_delivery_date] DATE NULL,
    [end_date] DATE NULL,
    [status] VARCHAR(20) NOT NULL DEFAULT ''''ACTIVE'''' CHECK ([status] IN (''''ACTIVE'''', ''''PAUSED'''', ''''CANCELLED'''', ''''EXPIRED'''')),
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[orders] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [order_code] VARCHAR(20) NULL,
    [customer_id] BIGINT NOT NULL REFERENCES dbo.[users]([id]),
    [status] VARCHAR(20) NOT NULL DEFAULT ''''CART'''' CHECK ([status] IN (''''CART'''', ''''PLACED'''', ''''AWAITING_PAYMENT'''', ''''CONFIRMED'''', ''''COMPLETED'''', ''''CANCELLED'''', ''''EXPIRED'''')),
    [recipient_name] NVARCHAR(100) NULL,
    [recipient_phone] VARCHAR(20) NULL,
    [delivery_address] NVARCHAR(500) NULL,
    [voucher_id] BIGINT NULL REFERENCES dbo.[promotions]([id]),
    [points_used] INT NOT NULL DEFAULT 0,
    [item_total] DECIMAL(15,2) NOT NULL DEFAULT 0,
    [discount_amount] DECIMAL(15,2) NOT NULL DEFAULT 0,
    [delivery_fee] DECIMAL(15,2) NOT NULL DEFAULT 0,
    [payable_amount] DECIMAL(15,2) NOT NULL DEFAULT 0,
    [payment_method] VARCHAR(20) NULL CHECK ([payment_method] IN (''''COD'''', ''''CARD'''', ''''E_WALLET'''')),
    [estimated_delivery_at] DATETIME2 NULL,
    [subscription_id] BIGINT NULL REFERENCES dbo.[subscriptions]([id]),
    [placed_at] DATETIME2 NULL,
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[seller_orders] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [order_id] BIGINT NOT NULL REFERENCES dbo.[orders]([id]),
    [store_id] BIGINT NOT NULL REFERENCES dbo.[stores]([id]),
    [status] VARCHAR(20) NOT NULL DEFAULT ''''CART'''' CHECK ([status] IN (''''CART'''', ''''NEW'''', ''''ACCEPTED'''', ''''REJECTED'''', ''''READY'''', ''''ASSIGNED'''', ''''PICKED_UP'''', ''''IN_TRANSIT'''', ''''ARRIVING'''', ''''DELIVERED'''', ''''FAILED'''', ''''CANCELLED'''')),
    [subtotal] DECIMAL(15,2) NOT NULL DEFAULT 0,
    [commission] DECIMAL(15,2) NULL,
    [rejection_reason] NVARCHAR(500) NULL,
    [delivery_staff_id] BIGINT NULL REFERENCES dbo.[users]([id]),
    [cod_amount] DECIMAL(15,2) NOT NULL DEFAULT 0,
    [cold_chain_note] NVARCHAR(255) NULL,
    [evidence_required] BIT NOT NULL DEFAULT 0,
    [evidence_url] NVARCHAR(500) NULL,
    [failure_reason] NVARCHAR(500) NULL,
    [assigned_at] DATETIME2 NULL,
    [picked_up_at] DATETIME2 NULL,
    [delivered_at] DATETIME2 NULL,
    [failed_at] DATETIME2 NULL,
    [dispute_status] VARCHAR(20) NOT NULL DEFAULT ''''NONE'''' CHECK ([dispute_status] IN (''''NONE'''', ''''OPEN'''', ''''RESOLVED'''')),
    [dispute_note] NVARCHAR(1000) NULL,
    [dispute_decision] NVARCHAR(500) NULL,
    [settlement_status] VARCHAR(20) NOT NULL DEFAULT ''''UNSETTLED'''' CHECK ([settlement_status] IN (''''UNSETTLED'''', ''''SETTLED'''')),
    [settled_at] DATETIME2 NULL,
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[order_items] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [seller_order_id] BIGINT NOT NULL REFERENCES dbo.[seller_orders]([id]),
    [variant_id] BIGINT NOT NULL REFERENCES dbo.[variants]([id]),
    [batch_id] BIGINT NULL REFERENCES dbo.[batches]([id]),
    [quantity] INT NOT NULL CHECK (quantity > 0),
    [unit_price] DECIMAL(15,2) NOT NULL,
    [unit_cost] DECIMAL(15,2) NULL,
    [reservation_status] VARCHAR(20) NOT NULL DEFAULT ''''NONE'''' CHECK ([reservation_status] IN (''''NONE'''', ''''RESERVED'''', ''''CONSUMED'''', ''''RELEASED'''')),
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[transactions] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [type] VARCHAR(20) NOT NULL CHECK ([type] IN (''''PAYMENT'''', ''''COD'''', ''''REFUND'''', ''''ADJUSTMENT'''', ''''POINTS_EARN'''', ''''POINTS_REDEEM'''', ''''POINTS_EXPIRE'''')),
    [user_id] BIGINT NOT NULL REFERENCES dbo.[users]([id]),
    [order_id] BIGINT NULL REFERENCES dbo.[orders]([id]),
    [seller_order_id] BIGINT NULL REFERENCES dbo.[seller_orders]([id]),
    [method] VARCHAR(20) NULL,
    [amount] DECIMAL(15,2) NULL,
    [points] INT NULL,
    [gateway_ref] VARCHAR(100) NULL,
    [status] VARCHAR(20) NOT NULL CHECK ([status] IN (''''INITIATED'''', ''''PAID'''', ''''FAILED'''', ''''EXPIRED'''', ''''REQUESTED'''', ''''APPROVED'''', ''''REJECTED'''', ''''REFUNDED'''', ''''PENDING'''', ''''AVAILABLE'''', ''''REDEEMED'''', ''''COMPLETED'''')),
    [reason] NVARCHAR(500) NULL,
    [reconciled] BIT NOT NULL DEFAULT 0,
    [points_expire_at] DATE NULL,
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[reviews] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [order_item_id] BIGINT NOT NULL UNIQUE REFERENCES dbo.[order_items]([id]),
    [customer_id] BIGINT NOT NULL REFERENCES dbo.[users]([id]),
    [product_rating] TINYINT NOT NULL CHECK (product_rating BETWEEN 1 AND 5),
    [seller_rating] TINYINT NOT NULL CHECK (seller_rating BETWEEN 1 AND 5),
    [comment] NVARCHAR(2000) NULL,
    [report_count] INT NOT NULL DEFAULT 0,
    [moderation_status] VARCHAR(20) NOT NULL DEFAULT ''''PUBLISHED'''' CHECK ([moderation_status] IN (''''PUBLISHED'''', ''''REPORTED'''', ''''KEPT'''', ''''HIDDEN'''', ''''REMOVED'''')),
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    [updated_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[review_reports] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [review_id] BIGINT NOT NULL REFERENCES dbo.[reviews]([id]),
    [reporter_id] BIGINT NOT NULL REFERENCES dbo.[users]([id]),
    [reason] VARCHAR(30) NOT NULL CHECK ([reason] IN (''''SPAM'''', ''''OFFENSIVE'''', ''''MISLEADING'''', ''''OTHER'''')),
    [description] NVARCHAR(500) NULL,
    [status] VARCHAR(20) NOT NULL DEFAULT ''''OPEN'''' CHECK ([status] IN (''''OPEN'''', ''''RESOLVED'''')),
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[notifications] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [user_id] BIGINT NOT NULL REFERENCES dbo.[users]([id]),
    [event_type] VARCHAR(50) NOT NULL,
    [title] NVARCHAR(200) NOT NULL,
    [content] NVARCHAR(1000) NOT NULL,
    [link_url] NVARCHAR(500) NULL,
    [send_email] BIT NOT NULL DEFAULT 0,
    [email_status] VARCHAR(20) NULL CHECK ([email_status] IN (''''QUEUED'''', ''''SENT'''', ''''DELIVERED'''', ''''BOUNCED'''', ''''FAILED'''')),
    [email_retry_count] INT NOT NULL DEFAULT 0,
    [read_at] DATETIME2 NULL,
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE TABLE dbo.[audit_logs] (
    [id] BIGINT PRIMARY KEY IDENTITY,
    [actor_id] BIGINT NULL REFERENCES dbo.[users]([id]),
    [entity_type] VARCHAR(30) NOT NULL,
    [entity_id] BIGINT NOT NULL,
    [action] VARCHAR(50) NOT NULL,
    [old_value] NVARCHAR(MAX) NULL,
    [new_value] NVARCHAR(MAX) NULL,
    [note] NVARCHAR(500) NULL,
    [ip_address] VARCHAR(45) NULL,
    [created_at] DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);'';
    EXEC sys.sp_executesql N''SET XACT_ABORT ON;

ALTER TABLE dbo.settings ADD CONSTRAINT uq_settings_type_name UNIQUE(type,name);
CREATE UNIQUE INDEX ux_addresses_default ON dbo.addresses(user_id) WHERE is_default=1;
ALTER TABLE dbo.variants ADD CONSTRAINT uq_variants_option UNIQUE(listing_id,option_name);
ALTER TABLE dbo.batches ADD CONSTRAINT uq_batches_no UNIQUE(variant_id,batch_no),
 CONSTRAINT ck_batches_dates CHECK(harvest_date<=receipt_date AND receipt_date<expiry_date),
 CONSTRAINT ck_batches_stock CHECK(reserved_qty>=0 AND reserved_qty<=remaining_qty AND remaining_qty<=received_qty);
CREATE INDEX ix_batches_fefo ON dbo.batches(variant_id,status,expiry_date) INCLUDE(remaining_qty,reserved_qty,unit_cost);
CREATE UNIQUE INDEX ux_subscriptions_active ON dbo.subscriptions(customer_id,variant_id) WHERE status IN (''''ACTIVE'''',''''PAUSED'''');
ALTER TABLE dbo.subscriptions ADD CONSTRAINT ck_subscription_dates CHECK(end_date IS NULL OR end_date>=start_date);
CREATE UNIQUE INDEX ux_orders_code ON dbo.orders(order_code) WHERE order_code IS NOT NULL;
CREATE UNIQUE INDEX ux_orders_cart ON dbo.orders(customer_id) WHERE status=''''CART'''';
ALTER TABLE dbo.orders ADD subscription_occurrence DATE NULL, points_discount DECIMAL(15,2) NOT NULL DEFAULT 0,
 checkout_key UNIQUEIDENTIFIER NULL, payment_expires_at DATETIME2 NULL;'';
    EXEC sys.sp_executesql N''CREATE UNIQUE INDEX ux_orders_checkout ON dbo.orders(checkout_key) WHERE checkout_key IS NOT NULL;
CREATE UNIQUE INDEX ux_subscription_occurrence ON dbo.orders(subscription_id,subscription_occurrence) WHERE subscription_id IS NOT NULL AND subscription_occurrence IS NOT NULL;
ALTER TABLE dbo.orders ADD CONSTRAINT ck_order_money CHECK(points_used>=0 AND item_total>=0 AND discount_amount>=0 AND points_discount>=0 AND delivery_fee>=0 AND payable_amount>=0 AND payable_amount=item_total-discount_amount-points_discount+delivery_fee),
 CONSTRAINT ck_order_placed CHECK(status=''''CART'''' OR (order_code IS NOT NULL AND recipient_name IS NOT NULL AND recipient_phone IS NOT NULL AND delivery_address IS NOT NULL AND payment_method IS NOT NULL AND placed_at IS NOT NULL));
ALTER TABLE dbo.seller_orders ADD CONSTRAINT uq_seller_orders UNIQUE(order_id,store_id),
 CONSTRAINT ck_seller_money CHECK(subtotal>=0 AND cod_amount>=0 AND (commission IS NULL OR commission BETWEEN 0 AND subtotal));
ALTER TABLE dbo.order_items ADD CONSTRAINT ck_item_money CHECK(unit_price>0 AND (unit_cost IS NULL OR unit_cost>=0)),
 CONSTRAINT ck_item_allocation CHECK(reservation_status=''''NONE'''' OR (batch_id IS NOT NULL AND unit_cost IS NOT NULL));
ALTER TABLE dbo.promotions ADD CONSTRAINT ck_promotions_period CHECK(end_at>start_at),
 CONSTRAINT ck_promotions_owner CHECK((type=''''SELLER_CAMPAIGN'''' AND store_id IS NOT NULL) OR (type=''''VOUCHER'''' AND store_id IS NULL AND code IS NOT NULL)),
 CONSTRAINT ck_promotion_limits CHECK(min_spend>=0 AND budget_used>=0 AND used_count>=0 AND (discount_type<>''''PERCENT'''' OR discount_value<=100) AND (max_discount IS NULL OR max_discount>0) AND (budget_limit IS NULL OR budget_limit>=budget_used) AND (usage_limit IS NULL OR usage_limit>=used_count) AND (per_customer_limit IS NULL OR per_customer_limit>0));
CREATE UNIQUE INDEX ux_promotions_code ON dbo.promotions(code) WHERE code IS NOT NULL;
ALTER TABLE dbo.listings ADD CONSTRAINT ck_listing_plan CHECK((listing_type=''''PLAN'''' AND plan_frequency IS NOT NULL) OR (listing_type=''''PRODUCT'''' AND plan_frequency IS NULL AND enrollment_open=0)),
 CONSTRAINT ck_listing_images CHECK(image_urls IS NULL OR ISJSON(image_urls)=1),
 CONSTRAINT ck_listing_rating CHECK(rating BETWEEN 0 AND 5),
 CONSTRAINT ck_listing_rejection CHECK(status<>''''REJECTED'''' OR (rejection_reason IS NOT NULL AND LEN(LTRIM(RTRIM(rejection_reason)))>0));
ALTER TABLE dbo.stores ADD CONSTRAINT ck_store_rating CHECK(rating BETWEEN 0 AND 5);
ALTER TABLE dbo.transactions ADD provider VARCHAR(40) NULL, idempotency_key VARCHAR(150) NULL,
 parent_transaction_id BIGINT NULL REFERENCES dbo.transactions(id);'';
    EXEC sys.sp_executesql N''CREATE UNIQUE INDEX ux_transaction_gateway ON dbo.transactions(provider,gateway_ref) WHERE gateway_ref IS NOT NULL;
CREATE UNIQUE INDEX ux_transaction_idempotency ON dbo.transactions(idempotency_key) WHERE idempotency_key IS NOT NULL;
ALTER TABLE dbo.transactions ADD CONSTRAINT ck_transaction_value CHECK(
 (type IN (''''POINTS_EARN'''',''''POINTS_REDEEM'''',''''POINTS_EXPIRE'''') AND points IS NOT NULL AND points<>0 AND amount IS NULL) OR
 (type IN (''''PAYMENT'''',''''COD'''',''''REFUND'''',''''ADJUSTMENT'''') AND amount IS NOT NULL AND points IS NULL AND (type=''''ADJUSTMENT'''' OR amount>=0))),
 CONSTRAINT ck_refund_parent CHECK(type<>''''REFUND'''' OR (seller_order_id IS NOT NULL AND parent_transaction_id IS NOT NULL)),
 CONSTRAINT ck_gateway_provider CHECK(gateway_ref IS NULL OR provider IS NOT NULL);
ALTER TABLE dbo.review_reports ADD CONSTRAINT uq_review_reports UNIQUE(review_id,reporter_id);
ALTER TABLE dbo.reviews ADD CONSTRAINT ck_report_count CHECK(report_count>=0);
ALTER TABLE dbo.notifications ADD email_provider_ref VARCHAR(150) NULL, next_retry_at DATETIME2 NULL,
 dedup_key VARCHAR(150) NULL, CONSTRAINT ck_email_retries CHECK(email_retry_count>=0);'';
    EXEC sys.sp_executesql N''CREATE UNIQUE INDEX ux_notification_dedup ON dbo.notifications(dedup_key) WHERE dedup_key IS NOT NULL;

-- Supporting tables required by password recovery, promotion usage and callbacks.
CREATE TABLE dbo.password_reset_tokens (
 id BIGINT IDENTITY PRIMARY KEY,user_id BIGINT NOT NULL REFERENCES dbo.users(id),
 token_hash BINARY(32) NOT NULL UNIQUE,expires_at DATETIME2 NOT NULL,used_at DATETIME2 NULL,
 created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),CHECK(expires_at>created_at));
CREATE TABLE dbo.promotion_redemptions (
 id BIGINT IDENTITY PRIMARY KEY,promotion_id BIGINT NOT NULL REFERENCES dbo.promotions(id),
 order_id BIGINT NOT NULL REFERENCES dbo.orders(id),customer_id BIGINT NOT NULL REFERENCES dbo.users(id),
 discount_amount DECIMAL(15,2) NOT NULL CHECK(discount_amount>=0),
 status VARCHAR(20) NOT NULL DEFAULT ''''APPLIED'''' CHECK(status IN (''''APPLIED'''',''''RELEASED'''')),
 created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),UNIQUE(promotion_id,order_id));
CREATE INDEX ix_redemptions_customer ON dbo.promotion_redemptions(promotion_id,customer_id,status);
CREATE TABLE dbo.integration_events (
 id BIGINT IDENTITY PRIMARY KEY,provider VARCHAR(40) NOT NULL,event_key VARCHAR(150) NOT NULL,
 event_type VARCHAR(50) NOT NULL,payload NVARCHAR(MAX) NULL CHECK(payload IS NULL OR ISJSON(payload)=1),
 status VARCHAR(20) NOT NULL DEFAULT ''''RECEIVED'''' CHECK(status IN (''''RECEIVED'''',''''PROCESSED'''',''''REJECTED'''',''''FAILED'''')),
 received_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),processed_at DATETIME2 NULL,
 error_message NVARCHAR(500) NULL,UNIQUE(provider,event_key));
CREATE TABLE dbo.stock_movements (
 id BIGINT IDENTITY PRIMARY KEY,batch_id BIGINT NOT NULL REFERENCES dbo.batches(id),
 order_item_id BIGINT NULL REFERENCES dbo.order_items(id),actor_id BIGINT NULL REFERENCES dbo.users(id),
 movement_type VARCHAR(20) NOT NULL CHECK(movement_type IN (''''RECEIPT'''',''''RESERVE'''',''''RELEASE'''',''''CONSUME'''',''''ADJUST'''',''''EXPIRE'''')),
 quantity INT NOT NULL CHECK(quantity<>0),idempotency_key VARCHAR(150) NOT NULL UNIQUE,
 note NVARCHAR(500) NULL,created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME());
CREATE TABLE dbo.schema_versions(version INT PRIMARY KEY,description NVARCHAR(200) NOT NULL,applied_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME());
INSERT dbo.schema_versions VALUES(1,N''''Freshora initial schema; direct Customer/Seller registration'''',SYSUTCDATETIME());
CREATE INDEX ix_listings_browse ON dbo.listings(status,category_id,store_id);
CREATE INDEX ix_variants_listing ON dbo.variants(listing_id,status);
CREATE INDEX ix_orders_customer ON dbo.orders(customer_id,created_at);
CREATE INDEX ix_orders_expiry ON dbo.orders(status,payment_expires_at);
CREATE INDEX ix_seller_orders_store ON dbo.seller_orders(store_id,status);
CREATE INDEX ix_seller_orders_staff ON dbo.seller_orders(delivery_staff_id,status);
CREATE INDEX ix_order_items_batch ON dbo.order_items(batch_id,reservation_status);
CREATE INDEX ix_order_items_order ON dbo.order_items(seller_order_id);
CREATE INDEX ix_transactions_user ON dbo.transactions(user_id,type,created_at);
CREATE INDEX ix_transactions_refund ON dbo.transactions(parent_transaction_id,status);
CREATE INDEX ix_notifications_user ON dbo.notifications(user_id,read_at,created_at);
CREATE INDEX ix_notifications_retry ON dbo.notifications(email_status,next_retry_at);
CREATE INDEX ix_audit_entity ON dbo.audit_logs(entity_type,entity_id,created_at);'';
    EXEC sys.sp_executesql N''CREATE OR ALTER VIEW dbo.v_available_stock AS
 SELECT variant_id,SUM(CONVERT(BIGINT,remaining_qty-reserved_qty)) AS available_qty
 FROM dbo.batches WHERE status=''''AVAILABLE'''' AND expiry_date>CONVERT(date,SYSUTCDATETIME()) GROUP BY variant_id;'';
    EXEC sys.sp_executesql N''CREATE OR ALTER VIEW dbo.v_public_products AS
 SELECT l.id AS listing_id,l.store_id,s.store_name,l.title,l.description,l.image_urls,l.category_id,l.origin,
 v.id AS variant_id,v.option_name,v.unit,v.price,st.available_qty
 FROM dbo.listings l JOIN dbo.stores s ON s.id=l.store_id JOIN dbo.users u ON u.id=s.owner_id
 JOIN dbo.variants v ON v.listing_id=l.id JOIN dbo.v_available_stock st ON st.variant_id=v.id
 WHERE l.status=''''APPROVED'''' AND l.listing_type=''''PRODUCT'''' AND s.status=''''ACTIVE'''' AND u.status=''''ACTIVE'''' AND v.status=''''ACTIVE'''' AND st.available_qty>0;'';
    EXEC sys.sp_executesql N''CREATE OR ALTER TRIGGER dbo.tr_store_owner ON dbo.stores AFTER INSERT,UPDATE AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS(SELECT 1 FROM inserted i JOIN dbo.users u ON u.id=i.owner_id WHERE u.role<>''''SELLER'''') THROW 51001,''''Store owner must be a SELLER.'''',1;
 IF EXISTS(SELECT 1 FROM inserted i JOIN dbo.addresses a ON a.id=i.pickup_address_id WHERE a.user_id<>i.owner_id) THROW 51002,''''Pickup address must belong to store owner.'''',1;
END;'';
    EXEC sys.sp_executesql N''CREATE OR ALTER TRIGGER dbo.tr_user_store_role ON dbo.users AFTER UPDATE AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS(SELECT 1 FROM inserted i JOIN dbo.stores s ON s.owner_id=i.id WHERE i.role<>''''SELLER'''') THROW 51003,''''A store owner must retain SELLER role.'''',1;
END;'';
    EXEC sys.sp_executesql N''CREATE OR ALTER TRIGGER dbo.tr_item_ownership ON dbo.order_items AFTER INSERT,UPDATE AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS(SELECT 1 FROM inserted i JOIN dbo.variants v ON v.id=i.variant_id JOIN dbo.listings l ON l.id=v.listing_id JOIN dbo.seller_orders so ON so.id=i.seller_order_id LEFT JOIN dbo.batches b ON b.id=i.batch_id WHERE l.store_id<>so.store_id OR (i.batch_id IS NOT NULL AND b.variant_id<>i.variant_id)) THROW 51004,''''Order item store or batch mismatch.'''',1;
END;'';
    EXEC sys.sp_executesql N''CREATE OR ALTER TRIGGER dbo.tr_review_purchase ON dbo.reviews AFTER INSERT,UPDATE AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS(SELECT 1 FROM inserted i JOIN dbo.order_items oi ON oi.id=i.order_item_id JOIN dbo.seller_orders so ON so.id=oi.seller_order_id JOIN dbo.orders o ON o.id=so.order_id WHERE so.status<>''''DELIVERED'''' OR o.customer_id<>i.customer_id) THROW 51005,''''Only the buyer may review a delivered purchase.'''',1;
END;'';
    EXEC sys.sp_executesql N''CREATE OR ALTER TRIGGER dbo.tr_audit_append_only ON dbo.audit_logs INSTEAD OF UPDATE,DELETE AS
BEGIN THROW 51006,''''Audit logs are append-only.'''',1; END;'';
    EXEC sys.sp_executesql N''CREATE OR ALTER TRIGGER dbo.tr_transaction_immutable ON dbo.transactions AFTER UPDATE,DELETE AS
BEGIN
 SET NOCOUNT ON;
 IF EXISTS(SELECT id,type,amount,points,user_id,order_id,seller_order_id,parent_transaction_id FROM deleted EXCEPT SELECT id,type,amount,points,user_id,order_id,seller_order_id,parent_transaction_id FROM inserted) THROW 51007,''''Financial values cannot be overwritten or deleted; use adjustment entries.'''',1;
END;'';
    EXEC sys.sp_executesql N''CREATE OR ALTER PROCEDURE dbo.register_customer
 @email NVARCHAR(254),@password_hash VARCHAR(255),@full_name NVARCHAR(100),@phone VARCHAR(20)=NULL
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 SET @email=LOWER(LTRIM(RTRIM(@email)));
 IF @email IS NULL OR @email NOT LIKE ''''%_@_%._%'''' OR @full_name IS NULL OR LEN(LTRIM(RTRIM(@full_name)))=0 THROW 51100,''''Email and full name are required.'''',1;
 IF @password_hash IS NULL OR LEN(@password_hash)<>60 OR LEFT(@password_hash,4) NOT IN (''''$2a$'''',''''$2b$'''',''''$2y$'''') THROW 51101,''''Pass a BCrypt hash generated by the application.'''',1;
 INSERT dbo.users(email,password_hash,full_name,phone,role) VALUES(@email,@password_hash,@full_name,@phone,''''CUSTOMER'''');
 SELECT CONVERT(BIGINT,SCOPE_IDENTITY()) AS user_id;
END;'';
    EXEC sys.sp_executesql N''CREATE OR ALTER PROCEDURE dbo.register_seller
 @email NVARCHAR(254),@password_hash VARCHAR(255),@full_name NVARCHAR(100),@store_name NVARCHAR(100),@phone VARCHAR(20)=NULL
AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 SET @email=LOWER(LTRIM(RTRIM(@email)));
 IF @email IS NULL OR @email NOT LIKE ''''%_@_%._%'''' OR @full_name IS NULL OR LEN(LTRIM(RTRIM(@full_name)))=0 OR @store_name IS NULL OR LEN(LTRIM(RTRIM(@store_name)))=0 THROW 51100,''''Email, full name and store name are required.'''',1;
 IF @password_hash IS NULL OR LEN(@password_hash)<>60 OR LEFT(@password_hash,4) NOT IN (''''$2a$'''',''''$2b$'''',''''$2y$'''') THROW 51101,''''Pass a BCrypt hash generated by the application.'''',1;
 BEGIN TRY
 BEGIN TRANSACTION;
 INSERT dbo.users(email,password_hash,full_name,phone,role) VALUES(@email,@password_hash,@full_name,@phone,''''SELLER'''');
 DECLARE @id BIGINT=SCOPE_IDENTITY(),@tier BIGINT;
 SELECT TOP(1) @tier=id FROM dbo.settings WHERE type=''''SELLER_TIER'''' AND status=''''ACTIVE'''' ORDER BY priority,id;
 INSERT dbo.stores(owner_id,store_name,seller_tier_id,status) VALUES(@id,@store_name,@tier,''''ACTIVE'''');
 DECLARE @store BIGINT=SCOPE_IDENTITY();
 COMMIT;
 SELECT @id AS user_id,@store AS store_id;
 END TRY
 BEGIN CATCH
 IF @@TRANCOUNT>0 ROLLBACK;
 THROW;
 END CATCH
END;'';
    EXEC sys.sp_executesql N''SET XACT_ABORT ON;

INSERT dbo.settings(type,name,value,priority) VALUES
(''''CATEGORY'''',N''''Vietnamese Fruit'''',NULL,1),(''''CATEGORY'''',N''''Imported Fruit'''',NULL,2),
(''''SELLER_TIER'''',N''''Basic'''',N''''{"listing_limit":50,"commission_rate":0.05}'''',1),
(''''MEMBERSHIP_TIER'''',N''''Bronze'''',N''''{"minimum_spend":0}'''',1),
(''''POLICY'''',N''''reservation_timeout_minutes'''',N''''15'''',1),
(''''POLICY'''',N''''points_vnd_per_point'''',N''''1000'''',2),
(''''SERVICE_AREA'''',N''''Hà Nội'''',N''''{"delivery_fee":25000}'''',1);
INSERT dbo.users(email,password_hash,full_name,phone,role) VALUES
(''''admin@freshora.demo'''',''''$2a$12$QQIBMWN2H8V1DBSmf1dA0.vzkdqg5.4LnMH2xIMOwY9mapCGxJCKa'''',N''''Freshora Admin'''',''''0900000001'''',''''ADMIN''''),
(''''customer@freshora.demo'''',''''$2a$12$QQIBMWN2H8V1DBSmf1dA0.vzkdqg5.4LnMH2xIMOwY9mapCGxJCKa'''',N''''Khách hàng demo'''',''''0900000002'''',''''CUSTOMER''''),
(''''seller@freshora.demo'''',''''$2a$12$QQIBMWN2H8V1DBSmf1dA0.vzkdqg5.4LnMH2xIMOwY9mapCGxJCKa'''',N''''Nhà vườn Freshora'''',''''0900000003'''',''''SELLER''''),
(''''seller2@freshora.demo'''',''''$2a$12$QQIBMWN2H8V1DBSmf1dA0.vzkdqg5.4LnMH2xIMOwY9mapCGxJCKa'''',N''''Green Fruit Store'''',''''0900000004'''',''''SELLER''''),
(''''delivery@freshora.demo'''',''''$2a$12$QQIBMWN2H8V1DBSmf1dA0.vzkdqg5.4LnMH2xIMOwY9mapCGxJCKa'''',N''''Nhân viên giao hàng'''',''''0900000005'''',''''DELIVERY_STAFF'''');
INSERT dbo.addresses(user_id,recipient_name,phone,address_line,ward,district,city,is_default)
SELECT id,full_name,phone,N''''01 Đường Demo'''',N''''Phường Demo'''',N''''Quận Demo'''',N''''Hà Nội'''',1 FROM dbo.users WHERE role IN (''''CUSTOMER'''',''''SELLER'''');
INSERT dbo.stores(owner_id,store_name,pickup_address_id,seller_tier_id)
SELECT u.id,CASE WHEN u.email=''''seller@freshora.demo'''' THEN N''''Freshora Garden'''' ELSE N''''Green Fruit Store'''' END,a.id,3 FROM dbo.users u JOIN dbo.addresses a ON a.user_id=u.id WHERE u.role=''''SELLER'''';
INSERT dbo.listings(store_id,category_id,title,origin,description,status,image_urls) VALUES(1,1,N''''King Oranges'''',N''''Hà Giang'''',N''''Freshora demo product'''',''''APPROVED'''',N''''["/assets/img/fruits/oranges.jpg"]'''');
INSERT dbo.variants(listing_id,option_name,unit,price) VALUES(1,N''''1 kg bag'''',N''''kg'''',35000);
INSERT dbo.batches(variant_id,batch_no,origin,harvest_date,receipt_date,expiry_date,supplier_name,received_qty,remaining_qty,unit_cost) VALUES(1,''''DEMO-1'''',N''''Hà Giang'''',DATEADD(day,-2,CONVERT(date,SYSUTCDATETIME())),CONVERT(date,SYSUTCDATETIME()),DATEADD(day,14,CONVERT(date,SYSUTCDATETIME())),N''''Nhà cung cấp demo'''',100,100,21000);
INSERT dbo.listings(store_id,category_id,title,origin,description,status,image_urls) VALUES(1,2,N''''Fuji Apples'''',N''''Nhật Bản'''',N''''Freshora demo product'''',''''APPROVED'''',N''''["/assets/img/fruits/apples.jpg"]'''');
INSERT dbo.variants(listing_id,option_name,unit,price) VALUES(2,N''''1 kg bag'''',N''''kg'''',85000);
INSERT dbo.batches(variant_id,batch_no,origin,harvest_date,receipt_date,expiry_date,supplier_name,received_qty,remaining_qty,unit_cost) VALUES(2,''''DEMO-2'''',N''''Nhật Bản'''',DATEADD(day,-2,CONVERT(date,SYSUTCDATETIME())),CONVERT(date,SYSUTCDATETIME()),DATEADD(day,14,CONVERT(date,SYSUTCDATETIME())),N''''Nhà cung cấp demo'''',100,100,51000);
INSERT dbo.listings(store_id,category_id,title,origin,description,status,image_urls) VALUES(1,1,N''''Cat Mangoes'''',N''''Đồng Tháp'''',N''''Freshora demo product'''',''''APPROVED'''',N''''["/assets/img/fruits/mangoes.jpg"]'''');
INSERT dbo.variants(listing_id,option_name,unit,price) VALUES(3,N''''1 kg bag'''',N''''kg'''',65000);
INSERT dbo.batches(variant_id,batch_no,origin,harvest_date,receipt_date,expiry_date,supplier_name,received_qty,remaining_qty,unit_cost) VALUES(3,''''DEMO-3'''',N''''Đồng Tháp'''',DATEADD(day,-2,CONVERT(date,SYSUTCDATETIME())),CONVERT(date,SYSUTCDATETIME()),DATEADD(day,14,CONVERT(date,SYSUTCDATETIME())),N''''Nhà cung cấp demo'''',100,100,39000);
INSERT dbo.listings(store_id,category_id,title,origin,description,status,image_urls) VALUES(2,1,N''''Green Grapes'''',N''''Ninh Thuận'''',N''''Freshora demo product'''',''''APPROVED'''',N''''["/assets/img/fruits/grapes.jpg"]'''');
INSERT dbo.variants(listing_id,option_name,unit,price) VALUES(4,N''''1 kg bag'''',N''''kg'''',120000);
INSERT dbo.batches(variant_id,batch_no,origin,harvest_date,receipt_date,expiry_date,supplier_name,received_qty,remaining_qty,unit_cost) VALUES(4,''''DEMO-4'''',N''''Ninh Thuận'''',DATEADD(day,-2,CONVERT(date,SYSUTCDATETIME())),CONVERT(date,SYSUTCDATETIME()),DATEADD(day,14,CONVERT(date,SYSUTCDATETIME())),N''''Nhà cung cấp demo'''',100,100,72000);
INSERT dbo.listings(store_id,category_id,title,origin,description,status,image_urls) VALUES(2,1,N''''Green-Skin Pomelos'''',N''''Bến Tre'''',N''''Freshora demo product'''',''''APPROVED'''',N''''["/assets/img/fruits/pomelo.jpg"]'''');
INSERT dbo.variants(listing_id,option_name,unit,price) VALUES(5,N''''1 kg bag'''',N''''kg'''',55000);
INSERT dbo.batches(variant_id,batch_no,origin,harvest_date,receipt_date,expiry_date,supplier_name,received_qty,remaining_qty,unit_cost) VALUES(5,''''DEMO-5'''',N''''Bến Tre'''',DATEADD(day,-2,CONVERT(date,SYSUTCDATETIME())),CONVERT(date,SYSUTCDATETIME()),DATEADD(day,14,CONVERT(date,SYSUTCDATETIME())),N''''Nhà cung cấp demo'''',100,100,33000);
INSERT dbo.listings(store_id,category_id,title,origin,description,status,image_urls) VALUES(2,1,N''''Watermelon'''',N''''Long An'''',N''''Freshora demo product'''',''''APPROVED'''',N''''["/assets/img/fruits/watermelon.jpg"]'''');
INSERT dbo.variants(listing_id,option_name,unit,price) VALUES(6,N''''1 kg bag'''',N''''kg'''',25000);
INSERT dbo.batches(variant_id,batch_no,origin,harvest_date,receipt_date,expiry_date,supplier_name,received_qty,remaining_qty,unit_cost) VALUES(6,''''DEMO-6'''',N''''Long An'''',DATEADD(day,-2,CONVERT(date,SYSUTCDATETIME())),CONVERT(date,SYSUTCDATETIME()),DATEADD(day,14,CONVERT(date,SYSUTCDATETIME())),N''''Nhà cung cấp demo'''',100,100,15000);
INSERT dbo.promotions(type,name,code,discount_type,discount_value,max_discount,min_spend,budget_limit,usage_limit,per_customer_limit,start_at,end_at,status)
VALUES(''''VOUCHER'''',N''''Ưu đãi khai trương'''',''''FRESH10'''',''''PERCENT'''',10,50000,100000,1000000,100,1,SYSUTCDATETIME(),DATEADD(day,30,SYSUTCDATETIME()),''''RUNNING'''');
INSERT dbo.notifications(user_id,event_type,title,content) SELECT id,''''WELCOME'''',N''''Chào mừng đến Freshora'''',N''''Tài khoản đã sẵn sàng sử dụng.'''' FROM dbo.users;'';
COMMIT;
SELECT DB_NAME() AS database_name, COUNT(*) AS tables_created FROM sys.tables;
SELECT * FROM dbo.v_public_products;';
    PRINT N'FreshoraDB installed successfully.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    PRINT N'Installation failed. No existing database was deleted. Inspect the error before retrying.';
    THROW;
END CATCH;

END;

-- Integrated Iteration2.sql
EXEC sys.sp_executesql N'USE FreshoraDB;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH(''dbo.seller_orders'',''delivery_fee'') IS NULL
 EXEC(''ALTER TABLE dbo.seller_orders ADD delivery_fee decimal(15,2) NOT NULL DEFAULT 0, discount_amount decimal(15,2) NOT NULL DEFAULT 0, points_discount decimal(15,2) NOT NULL DEFAULT 0, commission_rate decimal(9,6) NULL, net_payable decimal(15,2) NULL'');
IF COL_LENGTH(''dbo.transactions'',''remaining_points'') IS NULL
 EXEC(''ALTER TABLE dbo.transactions ADD remaining_points int NULL, evidence_url nvarchar(500) NULL'');
IF NOT EXISTS(SELECT 1 FROM settings WHERE type=''POLICY'' AND name=''cancellation_window_minutes'')
 INSERT settings(type,name,value) VALUES(''POLICY'',''cancellation_window_minutes'',''30'');
IF NOT EXISTS(SELECT 1 FROM settings WHERE type=''POLICY'' AND name=''delivery_eta_hours'')
 INSERT settings(type,name,value) VALUES(''POLICY'',''delivery_eta_hours'',''24'');
IF NOT EXISTS(SELECT 1 FROM settings WHERE type=''POLICY'' AND name=''points_earn_vnd'')
 INSERT settings(type,name,value) VALUES(''POLICY'',''points_earn_vnd'',''10000'');
IF NOT EXISTS(SELECT 1 FROM settings WHERE type=''POLICY'' AND name=''points_expiry_days'')
 INSERT settings(type,name,value) VALUES(''POLICY'',''points_expiry_days'',''365'');
IF NOT EXISTS(SELECT 1 FROM settings WHERE type=''POLICY'' AND name=''refund_window_days'')
 INSERT settings(type,name,value) VALUES(''POLICY'',''refund_window_days'',''7'');
COMMIT;
IF COL_LENGTH(''dbo.seller_orders'',''points_used'') IS NULL ALTER TABLE dbo.seller_orders ADD points_used int NOT NULL DEFAULT 0;
IF COL_LENGTH(''dbo.transactions'',''provider_create_date'') IS NULL ALTER TABLE dbo.transactions ADD provider_create_date varchar(14) NULL;
IF COL_LENGTH(''dbo.order_items'',''product_title'') IS NULL ALTER TABLE dbo.order_items ADD product_title nvarchar(200) NULL, variant_name nvarchar(100) NULL;
IF COL_LENGTH(''dbo.seller_orders'',''store_name_snapshot'') IS NULL ALTER TABLE dbo.seller_orders ADD store_name_snapshot nvarchar(100) NULL;

IF NOT EXISTS(SELECT 1 FROM settings WHERE type=''POLICY'' AND name=''membership_period_days'') INSERT settings(type,name,value) VALUES(''POLICY'',''membership_period_days'',''365'');
';

-- Integrated EnglishFruitCatalog.sql
EXEC sys.sp_executesql N'USE FreshoraDB;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
UPDATE dbo.listings SET title=N''King Oranges'', image_urls=N''["/assets/img/fruits/oranges.jpg"]'' WHERE title IN (N''Cam sành'',N''King Oranges'');
UPDATE dbo.listings SET title=N''Fuji Apples'', image_urls=N''["/assets/img/fruits/apples.jpg"]'' WHERE title IN (N''Táo Fuji'',N''Fuji Apples'');
UPDATE dbo.listings SET title=N''Cat Mangoes'', image_urls=N''["/assets/img/fruits/mangoes.jpg"]'' WHERE title IN (N''Xoài cát'',N''Cat Mangoes'');
UPDATE dbo.listings SET title=N''Green Grapes'', image_urls=N''["/assets/img/fruits/grapes.jpg"]'' WHERE title IN (N''Nho xanh'',N''Green Grapes'');
UPDATE dbo.listings SET title=N''Green-Skin Pomelos'', image_urls=N''["/assets/img/fruits/pomelo.jpg"]'' WHERE title IN (N''Bưởi da xanh'',N''Green-Skin Pomelos'');
UPDATE dbo.listings SET title=N''Watermelon'', image_urls=N''["/assets/img/fruits/watermelon.jpg"]'' WHERE title IN (N''Dưa hấu'',N''Watermelon'');
UPDATE dbo.stores SET store_name=N''Green Fruit Store'' WHERE store_name=N''Trái cây Xanh'';
UPDATE dbo.variants SET option_name=N''1 kg bag'' WHERE option_name=N''Gói 1 kg'';
UPDATE dbo.listings SET description=N''Freshora demo product'' WHERE description=N''Sản phẩm demo Freshora'';
UPDATE dbo.settings SET name=N''Vietnamese Fruit'' WHERE name=N''Trái cây Việt Nam'';
UPDATE dbo.settings SET name=N''Imported Fruit'' WHERE name=N''Trái cây nhập khẩu'';
COMMIT;
';


-- Refund classification preserves rewards and settlement for duplicate payments.
EXEC sys.sp_executesql N'USE FreshoraDB;
IF COL_LENGTH(''dbo.transactions'',''refund_scope'') IS NULL ALTER TABLE dbo.transactions ADD refund_scope varchar(20) NOT NULL CONSTRAINT df_refund_scope DEFAULT ''PURCHASE'';
';

-- FE-01.2 (BA-03): the address an order was placed with, so only that address is locked while the order is open.
-- No hard FK: a completed order must not prevent the address from being deleted later.
EXEC sys.sp_executesql N'USE FreshoraDB;
IF COL_LENGTH(''dbo.orders'',''delivery_address_id'') IS NULL ALTER TABLE dbo.orders ADD delivery_address_id BIGINT NULL;
';
EXEC sys.sp_executesql N'USE FreshoraDB;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=''ix_orders_delivery_address'' AND object_id=OBJECT_ID(''dbo.orders''))
 CREATE INDEX ix_orders_delivery_address ON dbo.orders(delivery_address_id) WHERE delivery_address_id IS NOT NULL;
';

-- FE-05.1 (BA-12): at most this percentage of the discounted item total may be paid with points.
EXEC sys.sp_executesql N'USE FreshoraDB;
IF NOT EXISTS(SELECT 1 FROM settings WHERE type=''POLICY'' AND name=''points_max_percent'')
 INSERT settings(type,name,value) VALUES(''POLICY'',''points_max_percent'',''50'');
';

PRINT N'Freshora database installation/upgrade completed.';
