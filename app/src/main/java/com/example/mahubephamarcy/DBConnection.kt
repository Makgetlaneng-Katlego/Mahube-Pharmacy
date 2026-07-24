package com.example.mahubephamarcy

import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.mahubephamarcy.MainActivity.Order
import com.example.mahubephamarcy.MainActivity.OrderItem
import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.collections.forEach

class DBConnection {
    private val TAG = "DBConnection"
    private val dbHost = "192.168.1.18"
    private val dbConnectionURL = "jdbc:mysql://$dbHost:3307/pharmacy?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
    private val dbUser = "Android_app"
    private val dbPassword = "K@ts1234"

    fun insertUserTable(fullname: String, email: String, phoneNumber: String, pass: String): Int {
        var connection: Connection? = null
        Log.d(TAG, "Attempting to connect to: $dbConnectionURL")
        
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            val sql = "INSERT INTO users (fullname, email, phone_number, password, role) VALUES (?, ?, ?, ?, ?)"
            val statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            statement.setString(1, fullname)
            statement.setString(2, email)
            statement.setString(3, phoneNumber)
            
            // Hash the password before saving
            val hashedPass = BCrypt.hashpw(pass, BCrypt.gensalt())
            statement.setString(4, hashedPass)
            
            statement.setString(5, "customer")
            
            val rowsInserted = statement.executeUpdate()
            if (rowsInserted > 0) {
                val generatedKeys = statement.getGeneratedKeys()
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1)
                }
            }
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting user: ${e.message}")
            0
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    fun verifyUser(email: String, pass: String): MainActivity.User? {
        var connection: Connection? = null
        Log.d(TAG, "Attempting to verify user: $email")

        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)

            // Query by email only to get the stored hash
            val sql = "SELECT user_id, fullname, email, phone_number, password FROM users WHERE email = ?"
            val statement = connection.prepareStatement(sql)
            statement.setString(1, email)

            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                val storedHash = resultSet.getString("password")
                
                // Verify the password against the hash
                if (BCrypt.checkpw(pass, storedHash)) {
                    Log.d(TAG, "Login successful for: $email")
                    return MainActivity.User(
                        id = resultSet.getInt("user_id"),
                        fullName = resultSet.getString("fullname"),
                        email = resultSet.getString("email"),
                        phoneNumber = resultSet.getString("phone_number")
                    )
                } else {
                    Log.d(TAG, "Login failed: Incorrect password for $email")
                    null
                }
            } else {
                Log.d(TAG, "Login failed: User not found with email $email")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login error: ${e.message}")
            null
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    fun insertPrescription(userEmail: String, doctorName: String, datePrescribed: String, type: String, notes: String, fileData: ByteArray?): Int {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            val sql = "INSERT INTO prescription (user_email, doctor_name, date_prescribed, type, notes, file_data) VALUES (?, ?, ?, ?, ?, ?)"
            val statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            statement.setString(1, userEmail)
            statement.setString(2, doctorName)
            statement.setString(3, datePrescribed)
            statement.setString(4, type)
            statement.setString(5, notes)
            if (fileData != null) {
                statement.setBytes(6, fileData)
            } else {
                statement.setNull(6, java.sql.Types.LONGVARBINARY)
            }
            
            val rowsInserted = statement.executeUpdate()
            if (rowsInserted > 0) {
                val generatedKeys = statement.getGeneratedKeys()
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1)
                }
            }
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting prescription: ${e.message}")
            0
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    fun insertCartItem(userEmail: String, productId: String, productName: String, quantity: Int, price: Double): Boolean {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            val sql = "INSERT INTO cart (user_email, product_id, product_name, quantity, price) VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE quantity = quantity + ?"
            val statement = connection.prepareStatement(sql)
            statement.setString(1, userEmail)
            statement.setString(2, productId)
            statement.setString(3, productName)
            statement.setInt(4, quantity)
            statement.setDouble(5, price)
            statement.setInt(6, quantity)
            
            val rowsAffected = statement.executeUpdate()
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting cart item: ${e.message}")
            false
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    fun updateCartQuantity(userEmail: String, productId: String, newQuantity: Int): Boolean {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            val sql = if (newQuantity > 0) {
                "UPDATE cart SET quantity = ? WHERE user_email = ? AND product_id = ?"
            } else {
                "DELETE FROM cart WHERE user_email = ? AND product_id = ?"
            }
            
            val statement = connection.prepareStatement(sql)
            if (newQuantity > 0) {
                statement.setInt(1, newQuantity)
                statement.setString(2, userEmail)
                statement.setString(3, productId)
            } else {
                statement.setString(1, userEmail)
                statement.setString(2, productId)
            }
            
            val rowsAffected = statement.executeUpdate()
            rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cart quantity: ${e.message}")
            false
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    fun getProducts(limit: Int = 100): List<ProductData> {
        var connection: Connection? = null
        val productList = mutableListOf<ProductData>()
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            // Assuming the 'image' column now stores a URL or file path string
            val sql = "SELECT product_id, product_name, price, image FROM products LIMIT ?"
            val statement = connection.prepareStatement(sql)
            statement.setInt(1, limit)
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                val imagePath = resultSet.getString("image")

                Log.d(TAG, "Product fetched: ${resultSet.getString("product_name")}, Image path: $imagePath")
                productList.add(ProductData(
                    id = resultSet.getInt("product_id").toString(),
                    name = resultSet.getString("product_name"),
                    price = resultSet.getDouble("price"),
                    imagePath = imagePath
                ))
            }
            productList
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching products: ${e.message}")
            emptyList()
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    data class ProductData(val id: String, val name: String, val price: Double, val imagePath: String?)
    
    fun insertAddress(userId: Int, addressName: String, streetAddress: String, city: String, province: String, postalCode: String, deliveryIns: String): Boolean {
        var connection: Connection? = null
        Log.d(TAG, "Attempting to insert address for user: $userId")

        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            val sql = "INSERT INTO address (user_id, street_address, city, province, postal_code, address_name, delivery_instruction) VALUES (?, ?, ?, ?, ?, ?, ?)"
            val statement = connection.prepareStatement(sql)
            statement.setInt(1, userId)
            statement.setString(2, streetAddress)
            statement.setString(3, city)
            statement.setString(4, province)
            statement.setString(5, postalCode)
            statement.setString(6, addressName)
            statement.setString(7, deliveryIns)

            val rowsInserted = statement.executeUpdate()
            rowsInserted > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting address: ${e.message}")
            false
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    fun createOrder(customerId: Int, totalAmount: Double, status: String, shippingAddressId: Int, paymentMethod: String, paymentStatus: String = "PENDING", transactionId: String? = null, items: List<MainActivity.OrderItem>): Boolean {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            connection.autoCommit = false

            val orderSql = "INSERT INTO orders (customer_id, total_amount, status, shipping_address, payment_method, payment_status, transaction_id) VALUES (?, ?, ?, ?, ?, ?, ?)"
            val orderStatement = connection.prepareStatement(orderSql, java.sql.Statement.RETURN_GENERATED_KEYS)
            orderStatement.setInt(1, customerId)
            orderStatement.setDouble(2, totalAmount)
            orderStatement.setString(3, status)
            orderStatement.setInt(4, shippingAddressId)
            orderStatement.setString(5, paymentMethod)
            orderStatement.setString(6, paymentStatus)
            orderStatement.setString(7, transactionId)
            
            val affectedRows = orderStatement.executeUpdate()
            if (affectedRows == 0) throw SQLException("Creating order failed")

            val generatedKeys = orderStatement.getGeneratedKeys()
            if (!generatedKeys.next()) throw SQLException("Creating order failed, no ID obtained")
            val orderId = generatedKeys.getInt(1)

            val itemSql = "INSERT INTO order_items (order_id, product_id, quantity, price_at_time) VALUES (?, ?, ?, ?)"
            val itemStatement = connection.prepareStatement(itemSql)

            for (item in items) {
                itemStatement.setInt(1, orderId)
                itemStatement.setInt(2, item.productId.toIntOrNull() ?: 0)
                itemStatement.setInt(3, item.quantity)
                itemStatement.setDouble(4, item.price / item.quantity)
                itemStatement.addBatch()
            }
            itemStatement.executeBatch()

            connection.commit()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating order: ${e.message}")
            connection?.rollback()
            false
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    fun insertConsultation(patientName: String, email: String, phone: String, date: String, type: String, description: String): Long {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            val sql = "INSERT INTO consultation (fullname, email, phone_number, preferred_date, consultation_type, Description) VALUES (?, ?, ?, ?, ?, ?)"
            val statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
            statement.setString(1, patientName)
            statement.setString(2, email)
            statement.setString(3, phone)
            statement.setString(4, date)
            statement.setString(5, type)
            statement.setString(6, description)
            
            val rowsInserted = statement.executeUpdate()
            if (rowsInserted > 0) {
                val generatedKeys = statement.getGeneratedKeys()
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1)
                }
            }
            0
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting consultation: ${e.message}")
            0
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    fun getConsultations(userFullName: String): List<ConsultationData> {
        var connection: Connection? = null
        val consultations = mutableListOf<ConsultationData>()
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            // Using the singular table name 'consultation' and columns from your schema
            val sql = "SELECT consultation_type, preferred_date, Description, response FROM consultation WHERE fullname = ? ORDER BY consultation_id DESC"
            val statement = connection.prepareStatement(sql)
            statement.setString(1, userFullName)
            
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                val feedback = resultSet.getString("response")
                Log.d(TAG, "Consultation fetched: ${resultSet.getString("consultation_type")}, Feedback: $feedback")
                consultations.add(ConsultationData(
                    type = resultSet.getString("consultation_type"),
                    date = resultSet.getString("preferred_date"),
                    desc = resultSet.getString("Description"),
                    feedback = feedback
                ))
            }
            consultations
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching consultations: ${e.message}")
            emptyList()
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    fun getConsultationById(consultationId: Int): FullConsultationData? {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            val sql = "SELECT fullname, preferred_date, consultation_type, Description, response FROM consultation WHERE consultation_id = ?"
            val statement = connection.prepareStatement(sql)
            statement.setInt(1, consultationId)
            
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                FullConsultationData(
                    id = consultationId,
                    fullName = resultSet.getString("fullname"),
                    date = resultSet.getString("preferred_date"),
                    type = resultSet.getString("consultation_type"),
                    desc = resultSet.getString("Description"),
                    response = resultSet.getString("response")
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching consultation by ID: ${e.message}")
            null
        } finally {
            connection?.close()
        }
    }

    fun getPrescriptionById(prescriptionId: Int): PrescriptionData? {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            val sql = "SELECT user_email, doctor_name, date_prescribed, type, notes, response FROM prescription WHERE id = ?"
            val statement = connection.prepareStatement(sql)
            statement.setInt(1, prescriptionId)
            
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                PrescriptionData(
                    id = prescriptionId,
                    email = resultSet.getString("user_email"),
                    doctor = resultSet.getString("doctor_name"),
                    date = resultSet.getString("date_prescribed"),
                    type = resultSet.getString("type"),
                    notes = resultSet.getString("notes"),
                    response = resultSet.getString("response")
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching prescription by ID: ${e.message}")
            null
        } finally {
            connection?.close()
        }
    }

    fun getOrders(customerId: Int): List<OrderData> {
        var connection: Connection? = null
        val orders = mutableListOf<OrderData>()
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            val sql = "SELECT order_id, order_date, total_amount, status, payment_method, status, transaction_id FROM orders WHERE customer_id = ? ORDER BY order_date DESC"
            val statement = connection.prepareStatement(sql)
            statement.setInt(1, customerId)
            
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                orders.add(OrderData(
                    id = resultSet.getInt("order_id"),
                    date = resultSet.getString("order_date"),
                    total = resultSet.getDouble("total_amount"),
                    status = resultSet.getString("status"),
                    paymentMethod = resultSet.getString("payment_method"),
                    paymentStatus = resultSet.getString("status"),
                    transactionId = resultSet.getString("transaction_id")
                ))
            }
            orders
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching orders: ${e.message}")
            emptyList()
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    fun getOrderItems(orderId: Int): List<MainActivity.OrderItem> {
        var connection: Connection? = null
        val items = mutableListOf<MainActivity.OrderItem>()
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            
            val sql = "SELECT oi.product_id, p.product_name, oi.quantity, oi.price_at_time " +
                    "FROM order_items oi JOIN products p ON oi.product_id = p.product_id " +
                    "WHERE oi.order_id = ?"
            val statement = connection.prepareStatement(sql)
            statement.setInt(1, orderId)
            
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                items.add(MainActivity.OrderItem(
                    productId = resultSet.getInt("product_id").toString(),
                    productName = resultSet.getString("product_name"),
                    quantity = resultSet.getInt("quantity"),
                    price = resultSet.getDouble("price_at_time") * resultSet.getInt("quantity")
                ))
            }
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching order items: ${e.message}")
            emptyList()
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                Log.e(TAG, "Error closing connection: ${e.message}")
            }
        }
    }

    data class OrderData(val id: Int, val date: String, val total: Double, val status: String, val paymentMethod: String, val paymentStatus: String?, val transactionId: String?)

    data class AddressData(val id: Int, val name: String, val street: String, val city: String, val province: String, val postal: String)

    fun getAddresses(userId: Int): List<AddressData> {
        var connection: Connection? = null
        val addresses = mutableListOf<AddressData>()
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            val sql = "SELECT address_id, address_name, street_address, city, province, postal_code FROM address WHERE user_id = ?"
            val statement = connection.prepareStatement(sql)
            statement.setInt(1, userId)
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                addresses.add(AddressData(
                    id = resultSet.getInt("address_id"),
                    name = resultSet.getString("address_name"),
                    street = resultSet.getString("street_address"),
                    city = resultSet.getString("city"),
                    province = resultSet.getString("province"),
                    postal = resultSet.getString("postal_code")
                ))
            }
            addresses
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching addresses: ${e.message}")
            emptyList()
        } finally {
            try { connection?.close() } catch (e: SQLException) {}
        }
    }

    fun updatePaymentStatus(orderId: Int, status: String, transactionId: String): Boolean {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            val sql = "UPDATE orders SET payment_status = ?, transaction_id = ? WHERE order_id = ?"
            val statement = connection.prepareStatement(sql)
            statement.setString(1, status)
            statement.setString(2, transactionId)
            statement.setInt(3, orderId)
            statement.executeUpdate() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error updating payment status: ${e.message}")
            false
        } finally {
            connection?.close()
        }
    }

    fun logPaymentResponse(orderId: Int, response: String): Boolean {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            val sql = "INSERT INTO payment_logs (order_id, response_data) VALUES (?, ?)"
            val statement = connection.prepareStatement(sql)
            statement.setInt(1, orderId)
            statement.setString(2, response)
            statement.executeUpdate() > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error logging payment response: ${e.message}")
            false
        } finally {
            connection?.close()
        }
    }

    data class PrescriptionData(val id: Int, val email: String, val doctor: String, val date: String, val type: String, val notes: String?, val response: String?)

    data class FullConsultationData(val id: Int, val fullName: String, val date: String, val type: String, val desc: String, val response: String?)

    data class ConsultationData(val type: String, val date: String, val desc: String, val feedback: String?)

    fun getLatestConsultationFeedback(userFullName: String): String? {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            val sql = "SELECT response FROM consultation WHERE fullname = ? AND response IS NOT NULL ORDER BY consultation_id DESC LIMIT 1"
            val statement = connection.prepareStatement(sql)
            statement.setString(1, userFullName)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) resultSet.getString("response") else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest consultation feedback: ${e.message}")
            null
        } finally {
            connection?.close()
        }
    }

    fun getLatestPrescriptionFeedback(userEmail: String): String? {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            val sql = "SELECT response FROM prescription WHERE user_email = ? AND response IS NOT NULL ORDER BY id DESC LIMIT 1"
            val statement = connection.prepareStatement(sql)
            statement.setString(1, userEmail)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) resultSet.getString("response") else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest prescription feedback: ${e.message}")
            null
        } finally {
            connection?.close()
        }
    }

    fun getLatestOrderStatus(userId: Int): String? {
        var connection: Connection? = null
        return try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(dbConnectionURL, dbUser, dbPassword)
            val sql = "SELECT status FROM orders WHERE customer_id = ? ORDER BY order_id DESC LIMIT 1"
            val statement = connection.prepareStatement(sql)
            statement.setInt(1, userId)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) resultSet.getString("status") else null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching latest order status: ${e.message}")
            null
        } finally {
            connection?.close()
        }
    }

}
