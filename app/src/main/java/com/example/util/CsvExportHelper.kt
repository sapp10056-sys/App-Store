package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.ApplicationItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates and exports administrative CSV reports of all current applications in the catalog.
 */
object CsvExportHelper {

    fun exportApplicationsToCsv(context: Context, applications: List<ApplicationItem>) {
        if (applications.isEmpty()) {
            Toast.makeText(context, "No applications available to export.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = fileDateFormat.format(Date())

            val csvContent = buildString {
                // Header row
                appendLine("App ID,App Name,Developer Name,Developer ID,Developer Email,Category,Version Name,Version Code,Downloads,Rating,Status,Is Premium,Token Cost,Download URL,Created At,Updated At")

                // Data rows
                for (app in applications) {
                    val createdStr = dateFormat.format(Date(app.createdAt))
                    val updatedStr = dateFormat.format(Date(app.updatedAt))

                    append(escapeCsv(app.appId)).append(",")
                    append(escapeCsv(app.appName)).append(",")
                    append(escapeCsv(app.developerName)).append(",")
                    append(escapeCsv(app.developerId)).append(",")
                    append(escapeCsv(app.developerEmail)).append(",")
                    append(escapeCsv(app.category)).append(",")
                    append(escapeCsv(app.versionName)).append(",")
                    append(app.versionCode).append(",")
                    append(app.downloadCount).append(",")
                    append(app.rating).append(",")
                    append(escapeCsv(app.publicationStatus)).append(",")
                    append(if (app.isPremium) "Yes" else "No").append(",")
                    append(app.tokenCost).append(",")
                    append(escapeCsv(app.downloadUrl)).append(",")
                    append(escapeCsv(createdStr)).append(",")
                    appendLine(escapeCsv(updatedStr))
                }
            }

            // Write CSV to cache directory
            val fileName = "novastore_applications_report_$timestamp.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            // Share via FileProvider
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "NovaStore Applications Report ($timestamp)")
                putExtra(Intent.EXTRA_TEXT, "Attached is the NovaStore applications data export (${applications.size} items).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(sendIntent, "Export Applications CSV Report via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Toast.makeText(context, "Exported ${applications.size} applications to CSV!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeCsv(value: String): String {
        val safe = value.replace("\"", "\"\"")
        return "\"$safe\""
    }
}
