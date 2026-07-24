package com.example.mahubephamarcy

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface PaymentApi {
    @POST("api/payments/initiate")
    fun initiatePayment(@Body request: PaymentRequest): Call<PaymentResponse>

    @POST("api/orders/send-receipt")
    fun sendReceipt(@Body request: ReceiptRequest): Call<ReceiptResponse>

    @POST("api/cart/send-quote")
    fun sendCartQuote(@Body request: ReceiptRequest): Call<ReceiptResponse>
}

data class ReceiptRequest(
    val orderId: Int,
    val email: String,
    val items: List<ReceiptItem>,
    val total: Double
)

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val price: Double
)

data class ReceiptResponse(
    val success: Boolean,
    val message: String? = null
)

data class PaymentRequest(
    val orderId: Int,
    val amount: Double,
    val customerEmail: String
)

data class PaymentResponse(
    val success: Boolean,
    val checkoutUrl: String,
    val orderId: Int,
    val error: String? = null
)
