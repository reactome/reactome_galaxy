package org.reactome.cli

import com.opencsv.CSVReader
import java.io.File
import java.util.Locale

data class CSVData(
    val headers: List<String>,
    val rows: List<Map<String, String>>
)

fun readCsvData(data: String): CSVData {
    val reader = CSVReader(data.reader())
    val allRows = reader.readAll()

    if (allRows.isEmpty()) {
        return CSVData(emptyList(), emptyList())
    }

    val headers = allRows.first().toList()

    val rows = allRows.drop(1).map { row ->
        headers.indices.associate { index ->
            val value = if (index < row.size) row[index] else ""
            headers[index] to value
        }
    }

    return CSVData(headers, rows)
}

private const val PATHWAY_ID_FIELD = "Pathway identifier"
private const val PATHWAY_NAME_FIELD = "Pathway name"

private val NUMERIC_FIELDS = setOf("Entities ratio", "Entities pValue", "Entities FDR", "Reactions ratio")
private val FIELDS_TO_SKIP = setOf("Submitted entities found", "Mapped entities", "Found reaction identifiers")

class ReportGenerator(private val reactomeUrl: String) {

    fun generateHtmlReport(htmlReportFile: String, pathwaysReportData: String, token: String) {

        val csvData = readCsvData(pathwaysReportData)

        val htmlContent = StringBuilder()
        htmlContent.append("<html>")
        htmlContent.append("<head><title>Reactome Pathway Links</title></head>")

        htmlContent.append("<style>")
        htmlContent.append("body { font-family: Arial, sans-serif; margin: 20px; }")
        htmlContent.append("h1 { color: #333; }")
        htmlContent.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }")
        htmlContent.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
        htmlContent.append("th { background-color: #f4f4f4; color: #333; }")
        htmlContent.append("tr:nth-child(even) { background-color: #f9f9f9; }")
        htmlContent.append("tr:hover { background-color: #f1f1f1; }")
        htmlContent.append("a { color: #007bff; text-decoration: none; }")
        htmlContent.append("a:hover { text-decoration: underline; }")
        htmlContent.append("</style>")

        htmlContent.append("<body>")
        htmlContent.append("<h1>Pathways</h1>")
        htmlContent.append("<table border=\"1\">")
        htmlContent.append("<thead>")

        htmlContent.append("<tr>")
        csvData.headers.forEach { header ->
            when (header) {
                PATHWAY_NAME_FIELD -> {
                    htmlContent.append("<th>${header}</th>")
                    htmlContent.append("<th>Pathway Diagram</th>")
                }

                in FIELDS_TO_SKIP -> {}

                else -> {
                    htmlContent.append("<th>${header}</th>")
                }
            }
        }

        htmlContent.append("</tr>")
        htmlContent.append("</thead>")
        htmlContent.append("<tbody>")
        htmlContent.append("<tr>")

        csvData.rows.forEach { row ->
            val pathway = row[PATHWAY_ID_FIELD].orEmpty()
            val imageUrl = pathwayDiagramUrl(pathway, token)
            val smallImageUrl = "${imageUrl}&quality=5"
            val largeImageUrl = "${imageUrl}&quality=10"

            csvData.headers.forEach { header ->
                val value = row[header].orEmpty()

                when (header) {
                    PATHWAY_NAME_FIELD -> {
                        htmlContent.append("<td><a href=\"${pathwayBrowserLink(pathway, token)}\" target=\"_blank\">${value}</a></td>")
                        htmlContent.append("<td><a href=\"$largeImageUrl\" target=\"_blank\"><img src=\"$smallImageUrl\" alt=\"Pathway Diagram\" style=\"max-width: 100px; max-height: 100px;\"></a></td>")
                    }

                    in NUMERIC_FIELDS -> {
                        htmlContent.append("<td>${formatDouble(value)}</td>")
                    }


                    in FIELDS_TO_SKIP -> {
                        htmlContent.append("<td>skip</td>")
                    }

                    else -> {
                        htmlContent.append("<td>${value}</td>")
                    }
                }

            }
            htmlContent.append("</tr>")
        }

        htmlContent.append("</tbody>")
        htmlContent.append("</table>")
        htmlContent.append("</body>")
        htmlContent.append("</html>")

        File(htmlReportFile).writeText(htmlContent.toString())
    }

    fun pathwayDiagramUrl(pathwayId: String, token: String): String {
        return "${contentUrl()}/exporter/diagram/${pathwayId}.png?diagramProfile=Modern&token=$token&analysisProfile=Standard"
    }

    private fun contentUrl(): String {
        return "$reactomeUrl/ContentService"
    }

    fun pathwayBrowserLink(pathwayId: String, token: String): String {
        return "$reactomeUrl/PathwayBrowser/#/${pathwayId}&DTAB=AN&ANALYSIS=${token}"
    }

    private fun formatDouble(value: String): String {
        return String.format(Locale.US, "%.4e", value.toDoubleOrNull() ?: 0.0)
    }
}

