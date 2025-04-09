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
    private val reportPdfFile: Path? = null

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

    private fun analysisUrl(): String {
        return "$reactomeUrl/AnalysisService"
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