package com.example.gstbillingapp.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.gstbillingapp.data.local.entities.BusinessSettings
import com.example.gstbillingapp.data.local.entities.InvoiceEntity
import com.example.gstbillingapp.data.local.entities.InvoiceItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {
    fun generateInvoicePdf(invoice: InvoiceEntity, items: List<InvoiceItemEntity>, settings: BusinessSettings? = null): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // 1. Border and Main Layout
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.BLACK
        canvas.drawRect(20f, 20f, 575f, 822f, paint)

        // 2. Header Section
        paint.style = Paint.Style.FILL
        paint.textSize = 28f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("TAX INVOICE", 297f, 65f, paint)

        // Company Details (Top Left)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 16f
        canvas.drawText(settings?.companyName ?: "GST Billing App Pro", 50f, 105f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 10f
        paint.color = Color.DKGRAY
        canvas.drawText("GSTIN: ${settings?.gstNumber ?: "27AAAAA0000A1Z5"}", 50f, 122f, paint)
        canvas.drawText(settings?.address ?: "123, Finance Tower, Mumbai, Maharashtra - 400001", 50f, 137f, paint)
        canvas.drawText("Phone: ${settings?.phoneNumber ?: "+91 98765 43210"} | Email: ${settings?.email ?: "support@gstbilling.com"}", 50f, 152f, paint)

        // QR Code (Top Right) - Encodes Invoice ID and Total
        val qrContent = "Inv: ${invoice.invoiceNumber}\nTotal: ₹${invoice.grandTotal}"
        val qrBitmap = QrCodeGenerator.generateQrCode(qrContent, 100)
        canvas.drawBitmap(qrBitmap, 445f, 85f, null)

        // 3. Customer & Invoice Info Section
        paint.color = Color.BLACK
        paint.strokeWidth = 0.8f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(40f, 175f, 555f, 245f, paint)
        
        paint.style = Paint.Style.FILL
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText("BILL TO:", 50f, 195f, paint)
        paint.textSize = 13f
        canvas.drawText(invoice.customerName, 50f, 215f, paint)
        paint.isFakeBoldText = false
        paint.textSize = 10f
        canvas.drawText("GSTIN: ${invoice.customerGstin}", 50f, 230f, paint)

        // Invoice Metadata (Right side of box)
        paint.isFakeBoldText = true
        canvas.drawText("Invoice No:", 370f, 195f, paint)
        canvas.drawText("Date:", 370f, 215f, paint)
        paint.isFakeBoldText = false
        canvas.drawText(invoice.invoiceNumber, 440f, 195f, paint)
        val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date(invoice.date))
        canvas.drawText(dateStr, 440f, 215f, paint)

        // 4. Table Header
        var yPos = 270f
        paint.style = Paint.Style.FILL
        paint.color = Color.LTGRAY
        canvas.drawRect(40f, yPos, 555f, yPos + 25, paint)
        
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("Sr.", 50f, yPos + 17, paint)
        canvas.drawText("Description of Goods", 80f, yPos + 17, paint)
        canvas.drawText("Qty", 320f, yPos + 17, paint)
        canvas.drawText("Rate", 380f, yPos + 17, paint)
        canvas.drawText("Amount", 480f, yPos + 17, paint)

        // 5. Table Rows
        paint.isFakeBoldText = false
        yPos += 45f
        items.forEachIndexed { index, item ->
            canvas.drawText("${index + 1}", 50f, yPos, paint)
            canvas.drawText(item.itemName, 80f, yPos, paint)
            canvas.drawText("${item.quantity}", 320f, yPos, paint)
            canvas.drawText(String.format("%.2f", item.price), 380f, yPos, paint)
            val lineTotal = item.price * item.quantity
            canvas.drawText(String.format("%.2f", lineTotal), 480f, yPos, paint)
            
            yPos += 25f
            // Draw row separator
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.3f
            canvas.drawLine(40f, yPos - 18, 555f, yPos - 18, paint)
            paint.style = Paint.Style.FILL
            
            if (yPos > 650) { /* Page break logic simplified */ }
        }

        // 6. Summary Section
        yPos = 680f
        paint.strokeWidth = 0.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(350f, yPos, 555f, yPos, paint)
        
        paint.style = Paint.Style.FILL
        paint.textSize = 11f
        canvas.drawText("Sub-Total", 370f, yPos + 20, paint)
        canvas.drawText(String.format("₹ %.2f", invoice.subTotal), 480f, yPos + 20, paint)
        
        canvas.drawText("CGST", 370f, yPos + 40, paint)
        canvas.drawText(String.format("₹ %.2f", invoice.cgst), 480f, yPos + 40, paint)
        
        canvas.drawText("SGST", 370f, yPos + 60, paint)
        canvas.drawText(String.format("₹ %.2f", invoice.sgst), 480f, yPos + 60, paint)
        
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Grand Total", 370f, yPos + 90, paint)
        canvas.drawText(String.format("₹ %.2f", invoice.grandTotal), 480f, yPos + 90, paint)

        // 7. Signature Section
        yPos = 750f
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Notes: E. & O.E. Subject to Mumbai Jurisdiction.", 50f, yPos, paint)
        
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("For ${settings?.companyName ?: "GST Billing App Pro"}", 540f, yPos + 10, paint)
        
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.5f
        canvas.drawLine(400f, yPos + 55, 540f, yPos + 55, paint)
        
        paint.style = Paint.Style.FILL
        canvas.drawText("Authorized Signatory", 540f, yPos + 70, paint)

        // Footer
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.GRAY
        canvas.drawText("This is a computer generated invoice.", 297f, 810f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.getExternalFilesDir(null), "Invoice_${invoice.invoiceNumber}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        return file
    }
}
