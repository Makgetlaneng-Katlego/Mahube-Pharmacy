const express = require('express');
const bodyParser = require('body-parser');
const mysql = require('mysql2/promise');
const nodemailer = require('nodemailer');

const app = express();
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Database configuration (Matches DBConnection.kt)
const dbConfig = {
  host: '192.168.1.18',
  port: 3307,
  user: 'Android_app',
  password: 'K@ts1234',
  database: 'pharmacy'
};

// Email Configuration (Gmail SMTP)
const transporter = nodemailer.createTransport({
  host: 'smtp.gmail.com',
  port: 587,
  secure: false, // true for 465, false for other ports
  auth: {
    user: 'k3243730@gmail.com',
    pass: 'vwtf juui rtft nlam'
  }
});

// Payment Gateway Keys (Example: PayFast)
const PAYFAST_MERCHANT_ID = 'your_merchant_id';
const PAYFAST_MERCHANT_KEY = 'your_merchant_key';

// 1. Initialize Payment Intent
app.post('/api/payments/initiate', async (req, res) => {
  const { orderId, amount, customerEmail } = req.body;
  // ... (existing logic)
});

// 2. Send Email Receipt
app.post('/api/orders/send-receipt', async (req, res) => {
  const { orderId, email, items, total } = req.body;

  console.log(`Sending receipt for Order #${orderId} to ${email}`);

  // Format the items list for HTML
  const itemsHtml = items.map(item => `
    <tr>
      <td style="padding: 8px; border-bottom: 1px solid #ddd;">${item.name}</td>
      <td style="padding: 8px; border-bottom: 1px solid #ddd;">x${item.quantity}</td>
      <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: right;">R${item.price.toFixed(2)}</td>
    </tr>
  `).join('');

  const mailOptions = {
    from: '"Mahube Pharmacy" <k3243730@gmail.com>',
    to: email,
    subject: `Your Receipt for Order #${orderId}`,
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #eee; padding: 20px;">
        <h2 style="color: #1E40AF; text-align: center;">Mahube Pharmacy Receipt</h2>
        <p>Thank you for your order! Here is your receipt for <strong>Order #${orderId}</strong>.</p>

        <table style="width: 100%; border-collapse: collapse; margin-top: 20px;">
          <thead>
            <tr style="background-color: #f8f9fa;">
              <th style="padding: 8px; text-align: left;">Product</th>
              <th style="padding: 8px; text-align: left;">Qty</th>
              <th style="padding: 8px; text-align: right;">Price</th>
            </tr>
          </thead>
          <tbody>
            ${itemsHtml}
          </tbody>
          <tfoot>
            <tr>
              <td colspan="2" style="padding: 8px; font-weight: bold; text-align: right;">Delivery Fee:</td>
              <td style="padding: 8px; font-weight: bold; text-align: right;">R15.00</td>
            </tr>
            <tr>
              <td colspan="2" style="padding: 12px 8px; font-size: 18px; font-weight: bold; text-align: right;">Total:</td>
              <td style="padding: 12px 8px; font-size: 18px; font-weight: bold; text-align: right; color: #1E40AF;">R${total.toFixed(2)}</td>
            </tr>
          </tfoot>
        </table>

        <p style="margin-top: 30px; font-size: 12px; color: #777; text-align: center;">
          Mahube Pharmacy, Pretoria, South Africa.<br>
          If you have any questions, please contact us at info@mahubepharmacy.co.za
        </p>
      </div>
    `
  };

  try {
    await transporter.sendMail(mailOptions);
    res.json({ success: true, message: 'Receipt sent' });
  } catch (error) {
    console.error('Email error:', error);
    res.status(500).json({ success: false, error: 'Failed to send email' });
  }
});

// 3. Send Cart Quote (Manual Summary)
app.post('/api/cart/send-quote', async (req, res) => {
  const { email, items, total } = req.body;

  console.log(`Sending cart summary to ${email}`);

  const itemsHtml = items.map(item => `
    <tr>
      <td style="padding: 8px; border-bottom: 1px solid #ddd;">${item.name}</td>
      <td style="padding: 8px; border-bottom: 1px solid #ddd;">x${item.quantity}</td>
      <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: right;">R${item.price.toFixed(2)}</td>
    </tr>
  `).join('');

  const mailOptions = {
    from: '"Mahube Pharmacy" <k3243730@gmail.com>',
    to: email,
    subject: `Your Mahube Pharmacy Cart Summary`,
    html: `
      <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #eee; padding: 20px;">
        <h2 style="color: #1E40AF; text-align: center;">Cart Summary / Quote</h2>
        <p>Hello! Here is a summary of the items currently in your cart at Mahube Pharmacy.</p>

        <table style="width: 100%; border-collapse: collapse; margin-top: 20px;">
          <thead>
            <tr style="background-color: #f8f9fa;">
              <th style="padding: 8px; text-align: left;">Product</th>
              <th style="padding: 8px; text-align: left;">Qty</th>
              <th style="padding: 8px; text-align: right;">Price</th>
            </tr>
          </thead>
          <tbody>
            ${itemsHtml}
          </tbody>
          <tfoot>
            <tr>
              <td colspan="2" style="padding: 12px 8px; font-size: 18px; font-weight: bold; text-align: right;">Total (incl. Delivery):</td>
              <td style="padding: 12px 8px; font-size: 18px; font-weight: bold; text-align: right; color: #1E40AF;">R${total.toFixed(2)}</td>
            </tr>
          </tfoot>
        </table>

        <p style="margin-top: 20px;">
            Please note: Prices and availability are subject to change. This is not a confirmed order.
        </p>

        <p style="margin-top: 30px; font-size: 12px; color: #777; text-align: center;">
          Mahube Pharmacy, Pretoria, South Africa.<br>
          If you have any questions, please contact us at info@mahubepharmacy.co.za
        </p>
      </div>
    `
  };

  try {
    await transporter.sendMail(mailOptions);
    res.json({ success: true, message: 'Quote sent' });
  } catch (error) {
    console.error('Email error:', error);
    res.status(500).json({ success: false, error: 'Failed to send email' });
  }
});

// 4. Webhook (ITN - Instant Transaction Notification for PayFast)
// This endpoint is called by the payment gateway, NOT the mobile app.
app.post('/api/payments/webhook', async (req, res) => {
  const payload = req.body;
  const orderId = payload.m_payment_id || payload.custom_str1;
  const status = payload.payment_status;
  const transactionId = payload.pf_payment_id;

  console.log(`Received Webhook for Order #${orderId}, Status: ${status}`);

  try {
    const connection = await mysql.createConnection(dbConfig);

    if (status === 'COMPLETE') {
      await connection.execute(
        'UPDATE orders SET payment_status = ?, transaction_id = ? WHERE order_id = ?',
        ['PAID', transactionId, orderId]
      );
      console.log(`Order #${orderId} marked as PAID`);
    } else {
      await connection.execute(
        'UPDATE orders SET payment_status = ? WHERE order_id = ?',
        ['FAILED', orderId]
      );
    }

    await connection.end();
    res.sendStatus(200);
  } catch (error) {
    console.error('Webhook processing error:', error);
    res.status(500).send('Internal Server Error');
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Pharmacy Backend running on port ${PORT}`);
});
