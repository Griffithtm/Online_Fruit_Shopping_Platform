SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.seller_orders','delivery_fee') IS NULL
 EXEC('ALTER TABLE dbo.seller_orders ADD delivery_fee decimal(15,2) NOT NULL DEFAULT 0, discount_amount decimal(15,2) NOT NULL DEFAULT 0, points_discount decimal(15,2) NOT NULL DEFAULT 0, commission_rate decimal(9,6) NULL, net_payable decimal(15,2) NULL');
IF COL_LENGTH('dbo.transactions','remaining_points') IS NULL
 EXEC('ALTER TABLE dbo.transactions ADD remaining_points int NULL, evidence_url nvarchar(500) NULL');
IF NOT EXISTS(SELECT 1 FROM settings WHERE type='POLICY' AND name='cancellation_window_minutes')
 INSERT settings(type,name,value) VALUES('POLICY','cancellation_window_minutes','30');
IF NOT EXISTS(SELECT 1 FROM settings WHERE type='POLICY' AND name='delivery_eta_hours')
 INSERT settings(type,name,value) VALUES('POLICY','delivery_eta_hours','24');
IF NOT EXISTS(SELECT 1 FROM settings WHERE type='POLICY' AND name='points_earn_vnd')
 INSERT settings(type,name,value) VALUES('POLICY','points_earn_vnd','10000');
IF NOT EXISTS(SELECT 1 FROM settings WHERE type='POLICY' AND name='points_expiry_days')
 INSERT settings(type,name,value) VALUES('POLICY','points_expiry_days','365');
IF NOT EXISTS(SELECT 1 FROM settings WHERE type='POLICY' AND name='refund_window_days')
 INSERT settings(type,name,value) VALUES('POLICY','refund_window_days','7');
COMMIT;
IF COL_LENGTH('dbo.seller_orders','points_used') IS NULL ALTER TABLE dbo.seller_orders ADD points_used int NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.transactions','provider_create_date') IS NULL ALTER TABLE dbo.transactions ADD provider_create_date varchar(14) NULL;
IF COL_LENGTH('dbo.order_items','product_title') IS NULL ALTER TABLE dbo.order_items ADD product_title nvarchar(200) NULL, variant_name nvarchar(100) NULL;
IF COL_LENGTH('dbo.seller_orders','store_name_snapshot') IS NULL ALTER TABLE dbo.seller_orders ADD store_name_snapshot nvarchar(100) NULL;

IF NOT EXISTS(SELECT 1 FROM settings WHERE type='POLICY' AND name='membership_period_days') INSERT settings(type,name,value) VALUES('POLICY','membership_period_days','365');
