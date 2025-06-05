package org.reactome.cli

import com.opencsv.CSVReader
import java.io.File
import java.util.Locale

private const val PATHWAY_ID_FIELD = "Pathway identifier"
private const val PATHWAY_NAME_FIELD = "Pathway name"

private val NUMERIC_FIELDS = setOf("Entities ratio", "Entities pValue", "Entities FDR", "Reactions ratio")
private val FIELDS_TO_SKIP = setOf("Submitted entities found", "Mapped entities", "Found reaction identifiers")

data class CSVData(
    val headers: List<String>,
    val rows: List<Map<String, String>>
)

fun readCsvData(contents: String): CSVData {
    val reader = CSVReader(contents.reader())
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

class ReportGenerator(private val reactomeUrl: String) {

    fun generateHtmlReport(htmlReportFile: String, pathwaysReportData: String, token: String) {
        val csvData = readCsvData(pathwaysReportData)
        val report = generateReportFromCsvData(csvData, token)
        File(htmlReportFile).writeText(report)
    }

    fun generateReportFromCsvData(csvData: CSVData, token: String): String {
        val htmlContent = StringBuilder()
        htmlContent.append("<html>\n")
        htmlContent.append("<head><title>Reactome Pathway Links</title></head>\n")

        htmlContent.append("<style>\n")
        htmlContent.append("body { font-family: Arial, sans-serif; margin: 20px; }\n")
        htmlContent.append("h1 { color: #333; }\n")
        htmlContent.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }\n")
        htmlContent.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
        htmlContent.append("th { background-color: #f4f4f4; color: #333; }\n")
        htmlContent.append("tr:nth-child(even) { background-color: #f9f9f9; }\n")
        htmlContent.append("tr:hover { background-color: #f1f1f1; }\n")
        htmlContent.append("a { color: #007bff; text-decoration: none; }\n")
        htmlContent.append("a:hover { text-decoration: underline; }\n")
        htmlContent.append("</style>\n")

        htmlContent.append("<body>\n")
        htmlContent.append("<h1>Pathways</h1>\n")
        htmlContent.append("<table border=\"1\">\n")
        htmlContent.append("<thead>\n")

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

        htmlContent.append("</tr>\n")
        htmlContent.append("</thead>\n")
        htmlContent.append("<tbody>\n")
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


                    in FIELDS_TO_SKIP -> {}

                    else -> {
                        htmlContent.append("<td>${value}</td>")
                    }
                }

            }
            htmlContent.append("</tr>\n")
        }

        htmlContent.append("</tbody>\n")
        htmlContent.append("</table>\n")
        htmlContent.append("</body>\n")
        htmlContent.append("</html>\n")

        return htmlContent.toString()
    }

    private fun pathwayDiagramUrl(pathwayId: String, token: String): String {
        return "${contentUrl()}/exporter/diagram/${pathwayId}.png?diagramProfile=Modern&token=$token&analysisProfile=Standard"
    }

    private fun contentUrl(): String {
        return "$reactomeUrl/ContentService"
    }

    private fun pathwayBrowserLink(pathwayId: String, token: String): String {
        return "$reactomeUrl/PathwayBrowser/#/${pathwayId}&DTAB=AN&ANALYSIS=${token}"
    }

    private fun formatDouble(value: String): String {
        return String.format(Locale.US, "%.4e", value.toDoubleOrNull() ?: 0.0)
    }
}

