-- Mahube Pharmacy Database Schema

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    fullname VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(15),
    password VARCHAR(255) NOT NULL, -- Stores BCrypt hashes
    role VARCHAR(20) DEFAULT 'customer',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Products Table
CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    image VARCHAR(255), -- Stores URL or path to image
    description TEXT
);

-- 3. Address Table
CREATE TABLE IF NOT EXISTS address (
    address_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    address_name VARCHAR(50), -- e.g., Home, Work
    street_address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    postal_code VARCHAR(10) NOT NULL,
    delivery_instruction TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 4. Orders Table
CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'Processing',
    shipping_address INT NOT NULL,
    payment_method VARCHAR(50),
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    transaction_id VARCHAR(100),
    FOREIGN KEY (customer_id) REFERENCES users(user_id),
    FOREIGN KEY (shipping_address) REFERENCES address(address_id)
);

-- 5. Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    price_at_time DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- 6. Cart Table (Server-side persistence)
CREATE TABLE IF NOT EXISTS carts (
    cart_id INT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    product_id INT NOT NULL,
    product_name VARCHAR(100),
    quantity INT NOT NULL,
    price DECIMAL(10, 2),
    UNIQUE KEY unique_user_product (user_email, product_id)
);

-- 7. Consultation Table
CREATE TABLE IF NOT EXISTS consultation (
    consultation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fullname VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    phone_number VARCHAR(15),
    preferred_date DATE,
    consultation_type VARCHAR(50),
    Description VARCHAR(350),
    response VARCHAR(350), -- Increased length from 50 to match typical feedback
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Prescription Table
CREATE TABLE IF NOT EXISTS prescription (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    doctor_name VARCHAR(100),
    date_prescribed DATE,
    type VARCHAR(50),
    notes TEXT,
    file_data LONGBLOB, -- For storing PDF/Image binaries
    response TEXT,
    status VARCHAR(50) DEFAULT 'Pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. Payment Logs Table
CREATE TABLE IF NOT EXISTS payment_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    response_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- Sample Data (Optional)
-- INSERT INTO products (product_name, price, image) VALUES ('First Aid Kit', 150.00, 'first_aid_kit');
-- INSERT INTO products (product_name, price, image) VALUES ('Burns Cream', 85.00, 'burns_cream');
