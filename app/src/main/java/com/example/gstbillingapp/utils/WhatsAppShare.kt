package com.example.gstbillingapp.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.gstbillingapp.data.local.entities.InvoiceEntity
import com.example.gstbillingapp.data.local.entities.InvoiceItemEntity
import java.io.File
import java.util.Locale

object WhatsAppShare {
    private val PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun summaryText(invoice: InvoiceEntity, items: List<InvoiceItemEntity>): String {
        val sb = StringBuilder()
        sb.append("*Tax Invoice ${invoice.invoiceNumber}*\n")
        if (invoice.customerName.isNotBlank()) sb.append("To: ${invoice.customerName}\n")
        sb.append('\n')
        items.forEach {
            sb.append("${it.itemName}  ${it.quantity} x ${money(it.price)} (${trim(it.gstRate)}% GST)\n")
        }
        sb.append('\n')
        sb.append("Subtotal: Rs ${money(invoice.subTotal)}\n")
        sb.append("CGST: Rs ${money(invoice.cgst)}\n")
        sb.append("SGST: Rs ${money(invoice.sgst)}\n")
        sb.append("*Total: Rs ${money(invoice.grandTotal)}*\n")
        if (invoice.dueAmount > 0) sb.append("Due: Rs ${money(invoice.dueAmount)}\n")
        sb.append("\nThank you!")
        return sb.toString()
    }

    /** Sends the PDF + summary to WhatsApp (or WhatsApp Business); falls back to the normal share sheet. */
    fun share(context: Context, pdf: File, text: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdf)
        val installed = PACKAGES.firstOrNull { isInstalled(context, it) }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (installed != null) {
            send.setPackage(installed)
            context.startActivity(send)
        } else {
            context.startActivity(Intent.createChooser(send, "Share Invoice").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun isInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun money(v: Double) = String.format(Locale.US, "%.2f", v)
    private fun trim(v: Double) = if (v == Math.floor(v)) v.toLong().toString() else v.toString()
}
