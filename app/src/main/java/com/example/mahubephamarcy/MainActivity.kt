package com.example.mahubephamarcy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    data class Product(val id: String, val name: String, val price: Double)

    private val products = mapOf(
        "first_aid_kit" to Product("first_aid_kit", "First Aid Kit", 150.0),
        "burns_cream" to Product("burns_cream", "Burns Cream", 85.0),
        "grand_pa" to Product("grand_pa", "Grand-Pa", 45.0),
        "vitamin_c" to Product("vitamin_c", "Vitamin C", 120.0),
        "vicks_syrup" to Product("vicks_syrup", "Vicks Syrup", 65.0),
        "paracetamol" to Product("paracetamol", "Paracetamol", 35.0)
    )

    // Map of Product ID to Quantity
    private val cart = mutableMapOf<String, Int>()

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            Toast.makeText(this, "File selected: $uri", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.loging_page)
        findViewById<Button>(R.id.SigningBtn)?.setOnClickListener {
            showHomePage(it)
        }
    }

    fun showHomePage(view: View) {
        setContentView(R.layout.dashboard)
        val systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
    }

    fun showProfilePage(view: View) {
        setContentView(R.layout.profile_page)
    }

    fun showAboutPage(view: View) {
        setContentView(R.layout.about_page)
    }

    fun showCartPage(view: View) {
        setContentView(R.layout.cart_page)
        updateCartUI()
    }

    fun showServicePage(view: View) {
        setContentView(R.layout.services_page)
    }

    fun showUploadPresciptionPage(view: View) {
        setContentView(R.layout.upload_prescription)
    }

    fun showConsultationPage(view: View) {
        setContentView(R.layout.consultation_page)
    }

    fun showContactPage(view: View) {
        showConsultationPage(view)
    }

    fun showCreateAccountPage(view: View) {
        setContentView(R.layout.create_account)
    }

    fun addToCart(view: View) {
        val productId = view.tag?.toString() ?: return
        val product = products[productId] ?: return
        
        cart[productId] = cart.getOrDefault(productId, 0) + 1
        Toast.makeText(this, "${product.name} added to cart!", Toast.LENGTH_SHORT).show()
    }

    private fun updateCartUI() {
        val container = findViewById<LinearLayout>(R.id.cartItemsContainer) ?: return
        container.removeAllViews()

        var subtotal = 0.0
        var totalItems = 0

        for ((productId, quantity) in cart) {
            val product = products[productId] ?: continue
            val itemView = LayoutInflater.from(this).inflate(R.layout.cart_item_row, container, false)
            
            val nameTxt = itemView.findViewById<TextView>(R.id.cartItemName)
            val priceTxt = itemView.findViewById<TextView>(R.id.cartItemPrice)
            val countTxt = itemView.findViewById<TextView>(R.id.cartItemCount)
            val btnPlus = itemView.findViewById<TextView>(R.id.btnPlus)
            val btnMinus = itemView.findViewById<TextView>(R.id.btnMinus)

            nameTxt.text = product.name
            priceTxt.text = "R${String.format("%.2f", product.price)}"
            countTxt.text = quantity.toString()

            btnPlus.setOnClickListener {
                cart[productId] = quantity + 1
                updateCartUI()
            }

            btnMinus.setOnClickListener {
                if (quantity > 1) {
                    cart[productId] = quantity - 1
                } else {
                    cart.remove(productId)
                }
                updateCartUI()
            }

            container.addView(itemView)
            subtotal += product.price * quantity
            totalItems += quantity
        }

        val delivery = if (cart.isEmpty()) 0.0 else 15.0
        val total = subtotal + delivery

        findViewById<TextView>(R.id.cartItemsSummary)?.text = "$totalItems items in your cart"
        findViewById<TextView>(R.id.cartSubtotal)?.text = "R${String.format("%.2f", subtotal)}"
        findViewById<TextView>(R.id.cartDelivery)?.text = "R${String.format("%.2f", delivery)}"
        findViewById<TextView>(R.id.cartTotal)?.text = "R${String.format("%.2f", total)}"
    }

    fun selectFile(view: View) {
        pickFileLauncher.launch(arrayOf("image/*", "application/pdf"))
    }

    fun submitPrescription(view: View) {
        Toast.makeText(this, "Prescription submitted successfully!", Toast.LENGTH_LONG).show()
        showHomePage(view)
    }
}
