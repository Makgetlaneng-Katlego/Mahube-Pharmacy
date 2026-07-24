package com.example.mahubephamarcy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

class ProductAdapter(
    private var products: List<DBConnection.ProductData>,
    private val onAddToCart: (DBConnection.ProductData) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.productImage)
        val productName: TextView = view.findViewById(R.id.productName)
        val productPrice: TextView = view.findViewById(R.id.productPrice)
        val addToCartBtn: Button = view.findViewById(R.id.addToCartBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.productName.text = product.name
        holder.productPrice.text = "R${String.format("%.2f", product.price)}"

        // Load image using Coil from path or URL
        holder.productImage.load(product.imagePath) {
            crossfade(true)
            placeholder(R.drawable.icon_image)
            error(R.drawable.icon_image)
        }

        holder.addToCartBtn.setOnClickListener {
            onAddToCart(product)
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<DBConnection.ProductData>) {
        this.products = newProducts
        notifyDataSetChanged()
    }
}
