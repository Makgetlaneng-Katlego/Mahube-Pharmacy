package com.example.mahubephamarcy

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private val db = DBConnection()
    private val notificationHelper = NotificationHelper(context)
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("PharmacyPrefs", Context.MODE_PRIVATE)

    override fun doWork(): Result {
        val userId = inputData.getInt("userId", 0)
        val userEmail = inputData.getString("userEmail") ?: ""
        val userFullName = inputData.getString("userFullName") ?: ""

        if (userId == 0) return Result.failure()

        Log.d("NotificationWorker", "Checking for updates for user: $userFullName")

        // 1. Check Consultation Feedback
        val latestConsultation = db.getLatestConsultationFeedback(userFullName)
        if (latestConsultation != null) {
            val lastSeen = sharedPrefs.getString("last_consultation_feedback", "")
            if (latestConsultation != lastSeen) {
                notificationHelper.showNotification("Consultation Update", "You have new feedback on your consultation request.", 101)
                sharedPrefs.edit().putString("last_consultation_feedback", latestConsultation).apply()
            }
        }

        // 2. Check Prescription Response
        val latestPrescription = db.getLatestPrescriptionFeedback(userEmail)
        if (latestPrescription != null) {
            val lastSeen = sharedPrefs.getString("last_prescription_feedback", "")
            if (latestPrescription != lastSeen) {
                notificationHelper.showNotification("Prescription Update", "Your prescription has been processed.", 102)
                sharedPrefs.edit().putString("last_prescription_feedback", latestPrescription).apply()
            }
        }

        // 3. Check Order Status
        val latestStatus = db.getLatestOrderStatus(userId)
        if (latestStatus != null) {
            val lastSeen = sharedPrefs.getString("last_order_status", "")
            if (latestStatus != lastSeen) {
                notificationHelper.showNotification("Order Status Update", "Your order status is now: $latestStatus", 103)
                sharedPrefs.edit().putString("last_order_status", latestStatus).apply()
            }
        }

        return Result.success()
    }
}
