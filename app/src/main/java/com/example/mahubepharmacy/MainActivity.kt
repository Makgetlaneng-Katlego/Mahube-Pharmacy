package com.example.mahubepharmacy

import android.R.attr.value
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.Coil
import coil.request.ImageRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.mahubepharmacy.R.id.createUser
import com.example.mahubepharmacy.R.id.logOutBtn
import org.jetbrains.annotations.MustBeInvokedByOverriders
import java.net.InetAddress
import java.net.PasswordAuthentication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.clear
import kotlin.times


class MainActivity : ComponentActivity() {
    val DB= DBConnection()
    data class User(val id: Int = 0, val fullName: String = "", val email: String = "", val phoneNumber: String = "")
    private var currentUser: User? = null
    data class Product(val id: String, val name: String, val price: Double)
    data class OrderItem(val productId: String, val productName: String, val quantity: Int, val price: Double)
    data class Order(val id: String, val items: List<OrderItem>, val total: Double, val date: String, val status: String)

    private val products = mapOf(
        "first_aid_kit" to Product("first_aid_kit", "First Aid Kit", 150.0),
        "burns_cream" to Product("burns_cream", "Burns Cream", 85.0),
        "grand_pa" to Product("grand_pa", "Grand-Pa", 45.0),
        "vitamin_c" to Product("vitamin_c", "Vitamin C", 120.0),
        "vicks_syrup" to Product("vicks_syrup", "Vicks Syrup", 65.0),
        "paracetamol" to Product("paracetamol", "Paracetamol", 35.0)
    )

    private val cart = mutableMapOf<String, Int>()
    private val placedOrders = mutableListOf<com.example.mahubepharmacy.MainActivity.Order>()
    private val dynamicProductsCache = mutableMapOf<String, com.example.mahubepharmacy.MainActivity.Product>()
    private var savedAddresses = listOf<DBConnection.AddressData>()

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:3000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val paymentApi = retrofit.create(PaymentApi::class.java)

    private var selectedFileUri: String? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedFileUri = uri.toString()
            Toast.makeText(this, "File selected: $uri", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.loging_page)

        // Initialize Notifications
        NotificationHelper(this).createNotificationChannel()
        checkNotificationPermission()

        findViewById<Button>(R.id.SigningBtn)?.setOnClickListener {
            handleLogin(it)
        }
    }
    private fun checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    private fun scheduleNotificationWorker(user: com.example.mahubepharmacy.MainActivity.User) {
        val inputData = Data.Builder()
            .putInt("userId", user.id)
            .putString("userEmail", user.email)
            .putString("userFullName", user.fullName)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).setInputData(inputData).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PharmacyFeedbackWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        Log.d("MainActivity", "Scheduled notification worker for ${user.fullName}")
    }
    fun handleLogin(view: View) {
        val email = findViewById<EditText>(R.id.loginEmail)?.text.toString()
        val password = findViewById<EditText>(R.id.loginPassword)?.text.toString()
        val feedback = findViewById<TextView>(R.id.SigningFeedback)

        if (email.isEmpty() || password.isEmpty()) {
            feedback?.text = "Please enter email and password"
            return
        }

        feedback?.text = "Signing in..."

        Thread {
            val user = DB.verifyUser(email, password)
            runOnUiThread {
                if (user != null) {
                    currentUser = user
                    scheduleNotificationWorker(user)
                    showHomePage(view)
                } else {
                    feedback?.text = "Invalid email or password"
                }
            }
        }.start()
    }
    fun showHomePage(view: View) {
        setContentView(R.layout.dashboard)
        initializeDashboard()
    }
    private fun initializeDashboard() {
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.productsRecyclerView) ?: return

        val adapter = ProductAdapter(
            products = emptyList(),
            onAddToCart = { product -> syncProductToCart(product) },
            onItemClick = { product -> showProductDetailsPage(product) }
        )
        recyclerView.adapter = adapter

        Thread {

            val productsList = DB.getProducts(limit = 4)


            productsList.forEach {
                dynamicProductsCache[it.id] = Product(it.id, it.name, it.price)
            }

            runOnUiThread {
                if (productsList.isEmpty()) {
                    Log.d("MainActivity", "No products found in database")
                    Toast.makeText(this, "No products available in database", Toast.LENGTH_SHORT).show()
                }
                adapter.updateProducts(productsList)
            }
        }.start()
    }
    fun showProductsPage(view: View) {
        setContentView(R.layout.products_page)
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.allProductsRecyclerView) ?: return
        val searchInput = findViewById<EditText>(R.id.searchProductsInput)

        val adapter = ProductAdapter(
            products = emptyList(),
            onAddToCart = { product -> syncProductToCart(product) },
            onItemClick = { product -> showProductDetailsPage(product) }
        )
        recyclerView.adapter = adapter


        var fullList = emptyList<DBConnection.ProductData>()

        Thread {
            fullList = DB.getProducts(limit = 100)
            fullList.forEach {
                dynamicProductsCache[it.id] = Product(it.id, it.name, it.price)
            }
            runOnUiThread {
                adapter.updateProducts(fullList)
            }
        }.start()


        searchInput?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase()
                val filteredList = fullList.filter {
                    it.name.lowercase().contains(query)
                }
                adapter.updateProducts(filteredList)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
    fun showAllProductsPage(view: View) {
        showProductsPage(view)
    }
    private fun syncProductToCart(product: DBConnection.ProductData) {
        val userEmail = currentUser?.email ?: ""
        cart[product.id] = cart.getOrDefault(product.id, 0) + 1
        Toast.makeText(this, "${product.name} added to cart!", Toast.LENGTH_SHORT).show()

        if (userEmail.isNotEmpty()) {
            Thread {
                val success = DB.insertCartItem(userEmail, product.id, product.name, 1, product.price)
                if (!success) {
                    Log.e("MainActivity", "Failed to sync cart item to database")
                }
            }.start()
        }
    }
    fun showProductDetailsPage(product: DBConnection.ProductData) {
        setContentView(R.layout.product_details)

        findViewById<TextView>(R.id.detailCategory)?.text = "GENERAL"
        findViewById<TextView>(R.id.detailProductName)?.text = product.name
        findViewById<TextView>(R.id.detailProductSubtitle)?.text = "${product.name} · Available"
        findViewById<TextView>(R.id.detailProductPrice)?.text = "R${String.format("%.2f", product.price)}"
        findViewById<TextView>(R.id.detailDescription)?.text = "${product.name} is a high quality pharmacy product."
        findViewById<TextView>(R.id.btnAddToCartDetail)?.text = "Add to Cart · R${String.format("%.2f", product.price)}"

        val imgView = findViewById<ImageView>(R.id.detailProductImage)
        if (imgView != null && !product.imagePath.isNullOrEmpty()) {
            Coil.imageLoader(this).enqueue(
                ImageRequest.Builder(this)
                    .data(product.imagePath)
                    .target(imgView)
                    .placeholder(R.drawable.icon_image)
                    .error(R.drawable.icon_image)
                    .build()
            )
        }

        var qty = 1
        val qtyView = findViewById<TextView>(R.id.detailQuantityCount)
        findViewById<TextView>(R.id.btnDetailMinus)?.setOnClickListener {
            if (qty > 1) {
                qty--
                qtyView?.text = qty.toString()
                findViewById<TextView>(R.id.btnAddToCartDetail)?.text = "Add to Cart · R${String.format("%.2f", product.price * qty)}"
            }
        }
        findViewById<TextView>(R.id.btnDetailPlus)?.setOnClickListener {
            qty++
            qtyView?.text = qty.toString()
            findViewById<TextView>(R.id.btnAddToCartDetail)?.text = "Add to Cart · R${String.format("%.2f", product.price * qty)}"
        }

        findViewById<Button>(R.id.btnAddToCartDetail)?.setOnClickListener {
            repeat(qty) {
                syncProductToCart(product)
            }
        }
    }
    fun showProfilePage(view: View) {
        setContentView(R.layout.profile_page)

        currentUser?.let { user ->
            findViewById<TextView>(R.id.profileName)?.text = user.fullName
            findViewById<TextView>(R.id.profileEmail)?.text = user.email

            // Generate initials
            val initials = user.fullName.split(" ")
                .filter { it.isNotEmpty() }
                .map { it[0].uppercase() }
                .take(2)
                .joinToString("")

            findViewById<TextView>(R.id.userInitials)?.text = if (initials.isNotEmpty()) initials else "U"
        }
    }
    fun showEditAccountPage(view: View) {
        setContentView(R.layout.edit_account)

        currentUser?.let { user ->
            findViewById<EditText>(R.id.editFullName)?.setText(user.fullName)
            findViewById<EditText>(R.id.editEmail)?.setText(user.email)
            findViewById<EditText>(R.id.editPhone)?.setText(user.phoneNumber)
        }
    }
    fun saveAccountChanges(view: View) {
        val newName = findViewById<EditText>(R.id.editFullName)?.text.toString()
        val newEmail = findViewById<EditText>(R.id.editEmail)?.text.toString()
        val newPhone = findViewById<EditText>(R.id.editPhone)?.text.toString()
        val isTermsChecked = findViewById<CheckBox>(R.id.editTermsCheckbox)?.isChecked ?: true

        if (newName.isEmpty() || newEmail.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isTermsChecked) {
            Toast.makeText(this, "Please agree to the Terms of Service", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser?.id ?: 0

        // Update in-memory user
        currentUser = currentUser?.copy(fullName = newName, email = newEmail, phoneNumber = newPhone)
        Toast.makeText(this, "Saving account changes...", Toast.LENGTH_SHORT).show()

        // Asynchronously update MySQL database
        if (userId > 0) {
            Thread {
                val success = DB.updateUserTable(userId, newName, newEmail, newPhone)
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Account updated in database successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Note: Changes saved locally, database update failed.", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }

        showProfilePage(view)
    }

    fun showChangePasswordPage(view: View) {
        setContentView(R.layout.change_password)
    }

    fun handlePasswordUpdate(view: View) {
        val currentPass = findViewById<EditText>(R.id.currentPassword)?.text.toString()
        val newPass = findViewById<EditText>(R.id.newPassword)?.text.toString()
        val confirmNewPass = findViewById<EditText>(R.id.confirmNewPassword)?.text.toString()

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmNewPass.isEmpty()) {
            Toast.makeText(this, "Please fill in all password fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass.length < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass != confirmNewPass) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser?.id ?: 0

        if (userId > 0) {
            Toast.makeText(this, "Updating password...", Toast.LENGTH_SHORT).show()
            Thread {
                val success = DB.updateUserPassword(userId, newPass)
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                        showEditAccountPage(view)
                    } else {
                        Toast.makeText(this, "Failed to update password", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        } else {
            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
            showEditAccountPage(view)
        }
    }

    fun showAboutPage(view: View) {
        setContentView(R.layout.about_page)
    }

    fun showCartPage(view: View) {
        setContentView(R.layout.cart_page)
        updateCartUI()
        loadSavedAddresses()
        findViewById<Button>(R.id.checkoutBtn)?.setOnClickListener { checkout(it) }
    }

    private fun loadSavedAddresses() {
        val userId = currentUser?.id ?: 0
        if (userId == 0) return

        val spinner = findViewById<android.widget.Spinner>(R.id.addressSpinner) ?: return

        Thread {
            savedAddresses = DB.getAddresses(userId)
            runOnUiThread {
                val addressStrings = if (savedAddresses.isEmpty()) {
                    listOf("No saved addresses")
                } else {
                    savedAddresses.map { "${it.name}: ${it.street}, ${it.city}" }
                }

                val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, addressStrings)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }
        }.start()
    }

    fun showServicePage(view: View) {
        setContentView(R.layout.services_page)
    }

    fun showUploadPresciptionPage(view: View) {
        setContentView(R.layout.upload_prescription)
    }

    fun showConsultationPage(view: View) {
        setContentView(R.layout.consultation_page)

        // Pre-fill user name to ensure database matches
        findViewById<EditText>(R.id.consultationName)?.setText(currentUser?.fullName)
        findViewById<EditText>(R.id.consultationEmail)?.setText(currentUser?.email)

        // Setup Consultation Type Spinner based on the provided list
        val typeSpinner = findViewById<android.widget.Spinner>(R.id.consultationType)
        val types = arrayOf("Medication Advice", "Prescription Question", "Side Effects", "General Health Question")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner?.adapter = adapter

        // loadConsultationHistory()
    }

    /*private fun loadConsultationHistory() {
        val recyclerView = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.consultationHistoryRecyclerView) ?: return
        val userFullName = currentUser?.fullName ?: ""

        Log.d("MainActivity", "Loading history for: $userFullName")
        if (userFullName.isEmpty()) return

        val adapter = ConsultationAdapter(emptyList())
        recyclerView.adapter = adapter

        Thread {
            val history = DB.getConsultations(userFullName)
            Log.d("MainActivity", "History items found: ${history.size}")
            runOnUiThread {
                if (history.isEmpty()) {
                    Log.d("MainActivity", "No history found for $userFullName")
                }
                adapter.updateData(history)
            }
        }.start()
    }*/

    fun showContactPage(view: View) {
        showConsultationPage(view)
    }

    fun showOrderHistoryPage(view: View) {
        setContentView(R.layout.order_history)
        populateOrderHistory()
    }

    fun showCreateAccountPage(view: View) {
        setContentView(R.layout.create_account)
    }

    fun addToCart(view: View) {
        val productId = view.tag?.toString() ?: return
        val product = products[productId] ?: return
        val userEmail = currentUser?.email ?: ""

        cart[productId] = cart.getOrDefault(productId, 0) + 1
        Toast.makeText(this, "${product.name} added to cart!", Toast.LENGTH_SHORT).show()

        if (userEmail.isNotEmpty()) {
            Thread {
                val success = DB.insertCartItem(userEmail, productId, product.name, 1, product.price)
                if (!success) {
                    Log.e("MainActivity", "Failed to sync cart item to database")
                }
            }.start()
        }
    }

    private fun updateCartUI() {
        val container = findViewById<LinearLayout>(R.id.cartItemsContainer) ?: return
        container.removeAllViews()
        var subtotal = 0.0
        var totalItems = 0
        val userEmail = currentUser?.email ?: ""

        for ((productId, quantity) in cart) {
            val product = products[productId] ?: dynamicProductsCache[productId] ?: continue
            val itemView = LayoutInflater.from(this).inflate(R.layout.cart_item_row, container, false)
            itemView.findViewById<TextView>(R.id.cartItemName).text = product.name
            itemView.findViewById<TextView>(R.id.cartItemPrice).text = "R${String.format("%.2f", product.price)}"
            itemView.findViewById<TextView>(R.id.cartItemCount).text = quantity.toString()

            itemView.findViewById<TextView>(R.id.btnPlus).setOnClickListener {
                val newQty = quantity + 1
                cart[productId] = newQty
                updateCartUI()
                if (userEmail.isNotEmpty()) {
                    Thread { DB.updateCartQuantity(userEmail, productId, newQty) }.start()
                }
            }

            itemView.findViewById<TextView>(R.id.btnMinus).setOnClickListener {
                val newQty = quantity - 1
                if (newQty > 0) cart[productId] = newQty else cart.remove(productId)
                updateCartUI()
                if (userEmail.isNotEmpty()) {
                    Thread { DB.updateCartQuantity(userEmail, productId, newQty) }.start()
                }
            }

            container.addView(itemView)
            subtotal += product.price * quantity
            totalItems += quantity
        }
        val delivery = if (cart.isEmpty()) 0.0 else 15.0
        findViewById<TextView>(R.id.cartItemsSummary)?.text = "$totalItems items in your cart"
        findViewById<TextView>(R.id.cartSubtotal)?.text = "R${String.format("%.2f", subtotal)}"
        findViewById<TextView>(R.id.cartDelivery)?.text = "R${String.format("%.2f", delivery)}"
        findViewById<TextView>(R.id.cartTotal)?.text = "R${String.format("%.2f", subtotal + delivery)}"
    }

    fun handleEmailCart(view: View) {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val userEmail = currentUser?.email ?: ""
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Please log in to email your cart", Toast.LENGTH_SHORT).show()
            return
        }

        val orderItems = cart.map { (id, qty) ->
            val p = products[id] ?: dynamicProductsCache[id]!!
            OrderItem(p.id, p.name, qty, p.price * qty)
        }
        val subtotal = orderItems.sumOf { it.price }
        val total = subtotal + 15.0

        val receiptItems = orderItems.map { ReceiptItem(it.productName, it.quantity, it.price) }
        val request = ReceiptRequest(0, userEmail, receiptItems, total) // Using 0 for orderId as it's a quote

        Toast.makeText(this, "Sending cart summary to $userEmail...", Toast.LENGTH_SHORT).show()

        paymentApi.sendCartQuote(request).enqueue(object : retrofit2.Callback<ReceiptResponse> {
            override fun onResponse(call: retrofit2.Call<ReceiptResponse>, response: retrofit2.Response<ReceiptResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@MainActivity, "Summary sent successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to send summary", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<ReceiptResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun checkout(view: View) {
        if (cart.isEmpty()) {
            Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show()
            return
        }

        val userEmail = currentUser?.email ?: ""
        val userId = currentUser?.id ?: 0

        if (userId == 0) {
            Toast.makeText(this, "Please log in to place an order", Toast.LENGTH_SHORT).show()
            return
        }

        if (savedAddresses.isEmpty()) {
            Toast.makeText(this, "Please add a delivery address first", Toast.LENGTH_SHORT).show()
            return
        }

        val spinner = findViewById<android.widget.Spinner>(R.id.addressSpinner)
        val selectedIndex = spinner?.selectedItemPosition ?: -1

        if (selectedIndex == -1 || savedAddresses.isEmpty()) {
            Toast.makeText(this, "Please select a delivery address", Toast.LENGTH_SHORT).show()
            return
        }

        val shippingAddressId = savedAddresses[selectedIndex].id

        val orderItems = cart.map { (id, qty) ->
            val p = products[id] ?: dynamicProductsCache[id]!!
            OrderItem(p.id, p.name, qty, p.price * qty)
        }
        val subtotal = orderItems.sumOf { it.price }
        val total = subtotal + 15.0
        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val orderIdLocal = "#ORD-2026-${String.format("%03d", placedOrders.size + 1)}"

        val paymentMethod = if (findViewById<android.widget.RadioButton>(R.id.radioCash)?.isChecked == true) "Cash" else "Card"

        Toast.makeText(this, "Placing your order...", Toast.LENGTH_SHORT).show()

        Thread {
            // If Card, initial payment status is PENDING
            val initialPaymentStatus = if (paymentMethod == "Card") "PENDING" else "N/A"
            val orderId = DB.createOrder(userId, total, "Processing", shippingAddressId, paymentMethod, initialPaymentStatus, null, orderItems)

            /**  runOnUiThread {
            if (orderId > 0) {
            if (paymentMethod == "Card") {
            initiateCardPayment(orderId, total, userEmail)
            sendReceiptEmail(orderId, userEmail, orderItems, total)
            } else {
            finalizeOrder(orderId, orderIdLocal, orderItems, total, date, view, userEmail)
            }
            } else {
            Toast.makeText(this, "Failed to place order in database", Toast.LENGTH_LONG).show()
            }
            }*/
        }.start()
    }

    private fun sendReceiptEmail(orderId: Int, email: String, items: List<OrderItem>, total: Double) {
        val receiptItems = items.map { ReceiptItem(it.productName, it.quantity, it.price) }
        val request = ReceiptRequest(orderId, email, receiptItems, total)

        paymentApi.sendReceipt(request).enqueue(object : retrofit2.Callback<ReceiptResponse> {
            override fun onResponse(call: retrofit2.Call<ReceiptResponse>, response: retrofit2.Response<ReceiptResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d("MainActivity", "Receipt email sent successfully")
                } else {
                    Log.e("MainActivity", "Failed to send receipt email")
                }
            }

            override fun onFailure(call: retrofit2.Call<ReceiptResponse>, t: Throwable) {
                Log.e("MainActivity", "Network error sending receipt: ${t.message}")
            }
        })
    }

    private fun initiateCardPayment(orderId: Int, amount: Double, email: String) {
        val request = PaymentRequest(orderId, amount, email)
        paymentApi.initiatePayment(request).enqueue(object : retrofit2.Callback<PaymentResponse> {
            override fun onResponse(call: retrofit2.Call<PaymentResponse>, response: retrofit2.Response<PaymentResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()?.checkoutUrl
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    Toast.makeText(this@MainActivity, "Redirecting to secure payment...", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Payment initiation failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<PaymentResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun populateOrderHistory() {
        val container = findViewById<LinearLayout>(R.id.ordersContainer) ?: return
        val userId = currentUser?.id ?: 0

        if (userId == 0) return

        Toast.makeText(this, "Loading order history...", Toast.LENGTH_SHORT).show()

        Thread {
            val orders = DB.getOrders(userId)

            runOnUiThread {
                container.removeAllViews()
                if (orders.isEmpty()) {
                    val emptyMsg = TextView(this)
                    emptyMsg.text = "No orders yet."
                    emptyMsg.textAlignment = View.TEXT_ALIGNMENT_CENTER
                    emptyMsg.setPadding(0, 50, 0, 0)
                    container.addView(emptyMsg)
                    return@runOnUiThread
                }

                for (order in orders) {
                    val orderView = LayoutInflater.from(this).inflate(R.layout.item_order, container, false)
                    orderView.findViewById<TextView>(R.id.orderIdText).text = "#ORD-${order.id}"
                    orderView.findViewById<TextView>(R.id.orderStatusBadge).text = order.status
                    orderView.findViewById<TextView>(R.id.orderDateText).text = order.date
                    orderView.findViewById<TextView>(R.id.orderTotalText).text = "R${String.format("%.2f", order.total)}"

                    val payBadge = orderView.findViewById<TextView>(R.id.paymentStatusBadge)
                    payBadge.text = order.paymentStatus ?: "PENDING"
                    if (order.paymentStatus == "PAID") {
                        payBadge.setBackgroundResource(R.drawable.bg_status_delivered)
                        payBadge.setTextColor(getColor(R.color.verified_green_text))
                    } else if (order.paymentStatus == "FAILED") {
                        payBadge.setBackgroundResource(R.drawable.edit_box)
                        payBadge.setTextColor(android.graphics.Color.RED)
                    }

                    // Style the status badge based on status
                    val statusBadge = orderView.findViewById<TextView>(R.id.orderStatusBadge)
                    when (order.status.lowercase()) {
                        "delivered" -> {
                            statusBadge.setBackgroundResource(R.drawable.bg_status_delivered)
                            statusBadge.setTextColor(Color.parseColor("#2D5A27"))
                        }
                        "processing" -> {
                            statusBadge.setBackgroundResource(R.drawable.bg_status_processing)
                            statusBadge.setTextColor(Color.parseColor("#B45309"))
                        }
                        "shipped" -> {
                            statusBadge.setBackgroundResource(R.drawable.bg_status_shipped)
                            statusBadge.setTextColor(Color.parseColor("#1E40AF"))
                        }
                        else -> {
                            statusBadge.setBackgroundResource(R.drawable.bg_qty_btn)
                            statusBadge.setTextColor(Color.parseColor("#64748B"))
                        }
                    }

                    val itemsContainer = orderView.findViewById<LinearLayout>(R.id.orderItemsContainer)
                    itemsContainer.removeAllViews()

                    // Make entire order item card clickable to toggle details view
                    orderView.setOnClickListener {
                        if (itemsContainer.visibility == View.GONE) {
                            itemsContainer.visibility = View.VISIBLE
                        } else {
                            itemsContainer.visibility = View.GONE
                        }
                    }

                    // Fetch items for this order
                    Thread {
                        val items = DB.getOrderItems(order.id)
                        runOnUiThread {
                            orderView.findViewById<TextView>(R.id.orderIdText).text = "#ORD-${order.id} • ${items.sumOf { it.quantity }} items"
                            for (item in items) {
                                val pView = LayoutInflater.from(this).inflate(R.layout.item_order_product, itemsContainer, false)
                                pView.findViewById<TextView>(R.id.productNameText).text = item.productName
                                pView.findViewById<TextView>(R.id.productQuantityText).text = "Qty: ${item.quantity}"
                                pView.findViewById<TextView>(R.id.productPriceText).text = "R${String.format("%.2f", item.price)}"
                                itemsContainer.addView(pView)
                            }
                        }
                    }.start()

                    container.addView(orderView)
                }
            }
        }.start()
    }

    fun selectFile(view: View) {
        pickFileLauncher.launch(arrayOf("image/*", "application/pdf"))
    }

    fun submitPrescription(view: View) {
        val doctorName = findViewById<EditText>(R.id.doctorName)?.text.toString()
        val datePrescribed = findViewById<EditText>(R.id.prescriptionDate)?.text.toString()
        val notes = findViewById<EditText>(R.id.additionalNotes)?.text.toString()
        val type = findViewById<android.widget.Spinner>(R.id.prescriptionType)?.selectedItem?.toString() ?: "General"
        val userEmail = currentUser?.email ?: ""

        if (doctorName.isEmpty() || datePrescribed.isEmpty()) {
            Toast.makeText(this, "Please fill in doctor name and date", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Submitting prescription...", Toast.LENGTH_SHORT).show()

        val fileData = selectedFileUri?.let { uriString ->
            try {
                contentResolver.openInputStream(Uri.parse(uriString))?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
        }

        Thread {
            val prescriptionId = DB.insertPrescription(userEmail, doctorName, datePrescribed, type, notes, fileData)
            runOnUiThread {
                if (prescriptionId > 0) {
                    findViewById<LinearLayout>(R.id.prescriptionResultCard)?.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.resPrescriptionId)?.text = "Success! Prescription ID: #$prescriptionId"
                    Toast.makeText(this, "Prescription submitted successfully! ID: #$prescriptionId", Toast.LENGTH_LONG).show()

                    // Optional: delay then show home or let user see ID
                    // showHomePage(view)
                } else {
                    Toast.makeText(this, "Failed to upload prescription", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    fun CreateUser(view: View){
        setContentView(R.layout.create_account)
    }

    fun acceptUserName(view: View){
        val fullName = findViewById<EditText>(R.id.fullname).text.toString()
        val email = findViewById<EditText>(R.id.email).text.toString()
        val phoneNumber = findViewById<EditText>(R.id.phoneNumber).text.toString()
        val pass = findViewById<EditText>(R.id.Pass).text.toString()
        val confirmPass = findViewById<EditText>(R.id.Confirmpass).text.toString()
        val isTermsChecked = findViewById<CheckBox>(R.id.termsCheckbox).isChecked
        val response: TextView = findViewById(R.id.Response)

        val validation = validateUserInputs(fullName, email, phoneNumber, pass, confirmPass, isTermsChecked)
        if(validation == "Open dashboard"){
            currentUser = User(fullName = fullName, email = email, phoneNumber = phoneNumber)
            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
            showHomePage(view)

            // Run database operation in background thread
            Thread {
                val newUserId = DB.insertUserTable(fullName, email, phoneNumber, pass)
                if (newUserId > 0) {
                    currentUser = currentUser?.copy(id = newUserId)
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to save account to database", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        } else {
            response.text = validation
        }
    }

    fun validateUserInputs(fullname: String, email: String, numberBnr: String, pass: String, confirmPass: String, isTermsChecked: Boolean): String {
        if(fullname.isEmpty()) return "Please Enter your name"
        if(email.isEmpty()) return "Please Enter your email"
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Please enter a valid email"
        if(numberBnr.isEmpty()) return "Please Enter your phone number"
        if(pass.isEmpty()) return "Please enter your password"
        if(pass.length < 6) return "Password must be at least 6 characters"
        if(pass != confirmPass) return "Passwords do not match"
        if(!isTermsChecked) return "Please agree to the Terms of Service"
        return "Open dashboard"
    }

    fun submitConsultation(view: View) {
        val name = findViewById<EditText>(R.id.consultationName)?.text.toString()
        val email = findViewById<EditText>(R.id.consultationEmail)?.text.toString()
        val phone = findViewById<EditText>(R.id.consultationPhone)?.text.toString()
        val date = findViewById<EditText>(R.id.consultationDate)?.text.toString()
        val type = findViewById<android.widget.Spinner>(R.id.consultationType)?.selectedItem?.toString() ?: "General"
        val desc = findViewById<EditText>(R.id.consultationDescription)?.text.toString()

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Requesting consultation...", Toast.LENGTH_SHORT).show()

        Thread {
            val bookingId = DB.insertConsultation(name, email, phone, date, type, desc)
            runOnUiThread {
                if (bookingId > 0) {
                    val successCard = findViewById<LinearLayout>(R.id.consultationBookingSuccessCard)
                    val idText = findViewById<TextView>(R.id.resBookingId)

                    successCard?.visibility = View.VISIBLE
                    idText?.text = "Success! Consultation ID: #$bookingId"

                    Toast.makeText(this, "Consultation requested successfully! ID: #$bookingId", Toast.LENGTH_LONG).show()
                    //loadConsultationHistory() // Refresh the list

                    // Clear inputs
                    findViewById<EditText>(R.id.consultationName)?.setText("")
                    findViewById<EditText>(R.id.consultationEmail)?.setText("")
                    findViewById<EditText>(R.id.consultationPhone)?.setText("")
                    findViewById<EditText>(R.id.consultationDate)?.setText("")
                    findViewById<EditText>(R.id.consultationDescription)?.setText("")
                } else {
                    Toast.makeText(this, "Failed to submit request", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }


    fun showLoggingPage(view: View){
        setContentView(R.layout.loging_page)
    }
    fun showAddressPage(view: View) {
        setContentView(R.layout.add_address)


        val provinceSpinner = findViewById<android.widget.Spinner>(R.id.provinceSpinner)
        val provinces = arrayOf("Select Province", "Gauteng", "Limpopo", "Mpumalanga", "North West", "KwaZulu-Natal", "Free State", "Eastern Cape", "Western Cape", "Northern Cape")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, provinces)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        provinceSpinner?.adapter = adapter
    }

    fun handleAddressBack(view: View) {
        if (cart.isNotEmpty()) {
            showCartPage(view)
        } else {
            showProfilePage(view)
        }
    }

    fun saveAddress(view: View) {
        val addressName = findViewById<EditText>(R.id.addressName)?.text.toString()
        val street = findViewById<EditText>(R.id.streetAddress)?.text.toString()
        val city = findViewById<EditText>(R.id.city)?.text.toString()
        val postal = findViewById<EditText>(R.id.postalCode)?.text.toString()
        val province = findViewById<android.widget.Spinner>(R.id.provinceSpinner)?.selectedItem.toString()

        val userEmail = currentUser?.email ?: ""
        val userId = currentUser?.id ?: 0

        if (addressName.isEmpty() || street.isEmpty() || city.isEmpty() || postal.isEmpty() || province == "Select Province") {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val deliveryInstructions = findViewById<EditText>(R.id.deliveryInstructions)?.text.toString()

        Toast.makeText(this, "Saving address...", Toast.LENGTH_SHORT).show()

        Thread {
            var activeUserId = userId
            if (activeUserId == 0 && userEmail.isNotEmpty()) {
                activeUserId = DB.getUserIdByEmail(userEmail)
                if (activeUserId > 0) {
                    currentUser = currentUser?.copy(id = activeUserId)
                }
            }

            val success = DB.insertAddress(activeUserId, addressName, street, city, province, postal, deliveryInstructions)
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Address saved to database successfully!", Toast.LENGTH_SHORT).show()
                    if (cart.isNotEmpty()) {
                        showCartPage(view)
                    } else {
                        showProfilePage(view)
                    }
                } else {
                    Toast.makeText(this, "Failed to save address to database", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    private fun finalizeOrder(orderId: Int, orderIdLocal: String, orderItems: List<OrderItem>, total: Double, date: String, view: View, userEmail: String) {
        placedOrders.add(0, Order(orderIdLocal, orderItems, total, date, "Processing"))
        cart.clear()
        sendReceiptEmail(orderId, userEmail, orderItems, total)
        Thread {
            orderItems.forEach { item ->
                DB.updateCartQuantity(userEmail, item.productId, 0)
            }
        }.start()

        Toast.makeText(this, "Order placed successfully! Check your inbox.", Toast.LENGTH_LONG).show()
        showOrderHistoryPage(view)
    }
}



