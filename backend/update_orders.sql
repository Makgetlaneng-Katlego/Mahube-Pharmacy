-- SQL script to update the orders table for payment processing support

-- 1. Add payment_status column
-- This tracks if the order is PAID, PENDING, or FAILED
ALTER TABLE orders
ADD COLUMN payment_status VARCHAR(50) DEFAULT 'PENDING' AFTER payment_method;

-- 2. Add transaction_id column
-- This stores the unique ID returned by the payment gateway (e.g., PayFast or Stripe)
ALTER TABLE orders
ADD COLUMN transaction_id VARCHAR(100) AFTER payment_status;

-- 3. (Optional) Initialize existing orders
-- If you already have orders in the system, you might want to mark them as 'N/A' or 'PAID'
UPDATE orders SET payment_status = 'N/A' WHERE payment_status IS NULL;
