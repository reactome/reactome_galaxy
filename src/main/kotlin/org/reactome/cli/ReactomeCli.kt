package org.reactome.cli

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.io.StringWriter
import java.nio.file.Path
import java.util.Locale

private const val CLI_USER_AGENT = "Mozilla/5.0 (compatible; Reactome CLI/1.0)"

class ReactomeCli(

    private val httpClient: HttpClient,
    private val reactomeUrl: String,
    private val inputFile: String,
    private val includeInteractors: Boolean,
    private val pathwaysFile: Path? = null,
    private val entitiesFoundFile: Path? = null,
    private val entitiesNotFoundFile: Path? = null,
    private val resultJsonFile: Path? = null,
    private val reportPdfFile: Path? = null,
    private val htmlReportFile: Path? = null,

    ) {

    fun execute(): String {
        val responseBody = analyzeUniprot()
        val analysisResponse = parseResponse(responseBody)

        if (pathwaysFile != null) {
            downloadPathways(analysisResponse.summary.token, filename = pathwaysFile.toString())
        }

        if (entitiesFoundFile != null) {
            downloadEntitiesFound(analysisResponse.summary.token, filename = entitiesFoundFile.toString())
        }

        if (entitiesNotFoundFile != null) {
            downloadEntitiesNotFound(analysisResponse.summary.token, filename = entitiesNotFoundFile.toString())
        }

        if (resultJsonFile != null) {
            downloadResultJson(analysisResponse.summary.token, filename = resultJsonFile.toString())
        }

        if (reportPdfFile != null) {
            downloadReportPdf(analysisResponse.summary.token, filename = reportPdfFile.toString())
        }

        if (htmlReportFile != null) {
            generateHtmlReport(analysisResponse, htmlReportFile.toString())
        }

        // TODO is this redundant with the pathways csv?
        val reportGenerator = ReportGenerator()
        val stringWriter = StringWriter()
        reportGenerator.writeCsv(analysisResponse, stringWriter)
        return stringWriter.toString()
    }

    private fun analyzeUniprot(): String {
        return runBlocking {
            val url = pagedUrl(identifiersUrl(), pageSize = 20, page = 1)
            val response = httpClient.post(url) {
                contentType(ContentType.Text.Plain)
                userAgent(CLI_USER_AGENT)
                setBody(File(inputFile).readText())
            }

            if (response.status.value != 200) {
                throw Exception("Failed to submit gene list: ${response.status.value} - ${response.body<String>()}")
            }

            response.body()
        }
    }

    private fun parseResponse(response: String): AnalysisResponse {
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString<AnalysisResponse>(response)
    }

    private fun downloadEntitiesFound(token: String, resource: ResourceType = ResourceType.TOTAL, filename: String) {
        val url = "${analysisUrl()}/download/${token}/entities/found/${resource}/entities_found.csv"
        val content = getFileContent(url)
        writeToDisk(filename, content)
    }

    private fun downloadEntitiesNotFound(token: String, filename: String) {
        val url = "${analysisUrl()}/download/${token}/entities/notfound/entities_not_found.csv"
        val content = getFileContent(url)
        writeToDisk(filename, content)
    }

    private fun downloadPathways(token: String, resource: ResourceType = ResourceType.TOTAL, filename: String) {
        val url = "${analysisUrl()}/download/${token}/pathways/${resource}/pathways.csv"
        val content = getFileContent(url)
        writeToDisk(filename, content)
    }

    private fun downloadResultJson(token: String, filename: String) {
        val url = "$reactomeUrl/AnalysisService/download/${token}/result.json"
        val content = getFileContent(url)
        writeToDisk(filename, content)
    }

    private fun downloadReportPdf(token: String, species: String = "Homo%20sapiens", filename: String) {
        val url = "${analysisUrl()}/report/${token}/${species}/report.pdf"
        val content = getFileContent(url)
        writeToDisk(filename, content)
    }

    private fun getFileContent(url: String): ByteArray {
        return runBlocking {
            val response = httpClient.get(url) {
                contentType(ContentType.Text.Plain)
                userAgent(CLI_USER_AGENT)
                header("Accept", "text/html,application/xhtml+xml,application/xml,application/pdf;q=0.9,*/*;q=0.8")
                header("Accept-Encoding", "gzip, deflate, br")
                header("Accept-Language", "en-US,en;q=0.9")
            }

            if (response.status.value != 200) {
                throw Exception("Request failed: ${url} ${response.status.value} - ${response.body<String>()}")
            }

            response.body<ByteArray>()
        }
    }

    private fun generateHtmlReport(analysisResponse: AnalysisResponse, htmlReportFile: String) {
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
        htmlContent.append("<h1>Interactive Pathway Links</h1>")
        htmlContent.append("<table border=\"1\">")
        htmlContent.append("<thead>")
        htmlContent.append("<tr>")
        htmlContent.append("<th>Pathway name</th>")
        htmlContent.append("<th>Pathway Diagram</th>")
        htmlContent.append("<th>#Entities found</th>")
        htmlContent.append("<th>#Entities total</th>")
        htmlContent.append("<th>Entities ratio</th>")
        htmlContent.append("<th>Entities pValue</th>")
        htmlContent.append("<th>Entities FDR</th>")
        htmlContent.append("<th>#Reactions found</th>")
        htmlContent.append("<th>#Reactions total</th>")
        htmlContent.append("<th>Reactions ratio</th>")
        htmlContent.append("<th>Species identifier</th>")
        htmlContent.append("<th>Species name</th>")
        htmlContent.append("</tr>")
        htmlContent.append("</thead>")
        htmlContent.append("<tbody>")

        val token = analysisResponse.summary.token
        analysisResponse.pathways.forEach { pathway ->
            val entitiesRatioStr = String.format(Locale.US, "%.12f", pathway.entities.ratio)
            val entitiesPValueStr = String.format(Locale.US, "%.2E", pathway.entities.pValue)
            val entitiesFdrStr = String.format(Locale.US, "%.12f", pathway.entities.fdr)
            val reactionsRatioStr = String.format(Locale.US, "%.12f", pathway.reactions.ratio)
            val imageUrl = pathwayDiagramUrl(pathway.stId, token)
            val smallImageUrl = "${imageUrl}&quality=5"
            val largeImageUrl = "${imageUrl}&quality=10"

            htmlContent.append("<tr>")
            htmlContent.append("<td><a href=\"${pathwayBrowserLink(pathway.stId, token)}\" target=\"_blank\">${pathway.name}</a></td>")
            htmlContent.append("<td><a href=\"$largeImageUrl\" target=\"_blank\"><img src=\"$smallImageUrl\" alt=\"Pathway Diagram\" style=\"max-width: 100px; max-height: 100px;\"></a></td>")
            htmlContent.append("<td>${pathway.entities.found}</td>")
            htmlContent.append("<td>${pathway.entities.total}</td>")
            htmlContent.append("<td>${entitiesRatioStr}</td>")
            htmlContent.append("<td>${entitiesPValueStr}</td>")
            htmlContent.append("<td>${entitiesFdrStr}</td>")
            htmlContent.append("<td>${pathway.reactions.found}</td>")
            htmlContent.append("<td>${pathway.reactions.total}</td>")
            htmlContent.append("<td>${reactionsRatioStr}</td>")
            htmlContent.append("<td>${pathway.species.taxId}</td>")
            htmlContent.append("<td>${pathway.species.name}</td>")
            htmlContent.append("</tr>")
        }

        htmlContent.append("</tbody>")
        htmlContent.append("</table>")
        htmlContent.append("</body>")
        htmlContent.append("</html>")

        writeToDisk(htmlReportFile, htmlContent.toString().toByteArray())
    }

    private fun analysisUrl(): String {
        return "$reactomeUrl/AnalysisService"
    }

    private fun contentUrl(): String {
        return "$reactomeUrl/ContentService"
    }

    private fun pathwayBrowserUrl(): String {
        return "$reactomeUrl/PathwayBrowser"
    }

    private fun pathwayBrowserLink(pathwayId: String, token: String): String {
        return "$reactomeUrl/PathwayBrowser/#/${pathwayId}&DTAB=AN&ANALYSIS=${token}"
    }

    private fun pathwayDiagramUrl(pathwayId: String, token: String): String {
        return "${contentUrl()}/exporter/diagram/${pathwayId}.png?diagramProfile=Modern&token=$token&analysisProfile=Standard"
    }

    private fun pagedUrl(url: String, pageSize: Int, page: Int): String {
        return "$url&pageSize=$pageSize&page=$page"
    }

    private fun identifiersUrl(): String {
        return "${analysisUrl()}/identifiers/projection?null&interactors=$includeInteractors"
    }

    private fun writeToDisk(fileName: String, content: ByteArray) {
        File(fileName).writeBytes(content)
    }
}