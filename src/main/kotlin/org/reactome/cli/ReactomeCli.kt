package org.reactome.cli

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File

private const val CLI_USER_AGENT = "Mozilla/5.0 (compatible; Reactome CLI/1.0)"

class ReactomeCli(
    private val httpClient: HttpClient,
    private val reactomeUrl: String,
) {

    fun analyseGenes(identifiersFile: String, includeInteractions: Boolean, options: CommonOptions) {
        val responseBody = analyzeGenes(identifiersFile, includeInteractions)
        val analysisResponse = parseResponse(responseBody)
        val token = analysisResponse.summary.token
        getAnalysisOutput(token, options)
    }

    fun analyseSpecies(speciesName: SpeciesName, options: CommonOptions) {
        val responseBody = analyzeSpecies(speciesName)
        val token = extractToken(responseBody)
        getAnalysisOutput(token, options)
    }

    fun analyseTissues(tissues: List<TissueName>, options: CommonOptions) {
        val responseBody = analyzeTissue(tissues)
        val token = extractToken(responseBody)
        getAnalysisOutput(token, options)
    }

    private fun getAnalysisOutput(token: String, options: CommonOptions) {

        if (options.pathwaysFile != null || options.htmlReportFile != null) {
            val pathwaysData = downloadPathways(token, filename = options.pathwaysFile.toString())
            val reportGenerator = ReportGenerator(reactomeUrl)
            val htmlReportFile = options.htmlReportFile?.toString()
            if (htmlReportFile != null) {
                reportGenerator.generateHtmlReport(htmlReportFile, pathwaysData, token)
            }
        }

        if (options.entitiesFoundFile != null) {
            downloadEntitiesFound(token, filename = options.entitiesFoundFile.toString())
        }

        if (options.entitiesNotFoundFile != null) {
            downloadEntitiesNotFound(token, filename = options.entitiesNotFoundFile.toString())
        }

        if (options.resultJsonFile != null) {
            downloadResultJson(token, filename = options.resultJsonFile.toString())
        }

        if (options.reportPdfFile != null) {
            downloadReportPdf(token, filename = options.reportPdfFile.toString())
        }
    }

    private fun analyzeGenes(inputFile: String, includeInteractors: Boolean): String {
        return runBlocking {
            val url = pagedUrl(identifiersUrl(includeInteractors), pageSize = 20, page = 1)
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

    private fun analyzeSpecies(speciesName: SpeciesName): String {
        return runBlocking {
            val url = pagedUrl(speciesUrl(speciesName), pageSize = 20, page = 1, joinWith = "?")
            val response = httpClient.get(url) {
                contentType(ContentType.Text.Plain)
                userAgent(CLI_USER_AGENT)
            }

            if (response.status.value != 200) {
                throw Exception("Failed to submit species: ${response.status.value} - ${response.body<String>()}")
            }

            response.body()
        }
    }

    private fun analyzeTissue(tissues: List<TissueName>): String {
        return runBlocking {
            val url = pagedUrl(tissueUrl(), pageSize = 20, page = 1)
            val response = httpClient.post(url) {
                contentType(ContentType.Text.Plain)
                userAgent(CLI_USER_AGENT)
                setBody(tissuePayload(tissues))
            }

            if (response.status.value != 200) {
                throw Exception("Failed to tissues: ${response.status.value} - ${response.body<String>()}")
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

    private fun downloadPathways(token: String, resource: ResourceType = ResourceType.TOTAL, filename: String): String {
        val url = "${analysisUrl()}/download/${token}/pathways/${resource}/pathways.csv"
        val content = getFileContent(url)

        writeToDisk(filename, content)
        return content
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

    private fun getFileContent(url: String): String {
        return runBlocking {
            val response = httpClient.get(url) {
                contentType(ContentType.Text.Plain)
                userAgent(CLI_USER_AGENT)
                header("Accept", "text/html,application/xhtml+xml,application/xml,application/pdf;q=0.9,*/*;q=0.8")
                header("Accept-Encoding", "gzip, deflate, br")
                header("Accept-Language", "en-US,en;q=0.9")
            }

            if (response.status.value != 200) {
                throw Exception("Request failed: $url ${response.status.value} - ${response.body<String>()}")
            }

            val contentType = response.contentType()
            val charset = contentType?.charset() ?: Charsets.UTF_8
            val contentString = String(response.body<ByteArray>(), charset)

            contentString
        }
    }

    private fun analysisUrl(): String {
        return "$reactomeUrl/AnalysisService"
    }

    private fun pagedUrl(url: String, pageSize: Int, page: Int, joinWith: String = "&"): String {
        return "$url${joinWith}pageSize=$pageSize&page=$page"
    }

    private fun identifiersUrl(includeInteractors: Boolean): String {
        return "${analysisUrl()}/identifiers/projection?null&interactors=$includeInteractors"
    }

    private fun speciesUrl(speciesName: SpeciesName): String {
        return "${analysisUrl()}/species/homoSapiens/${speciesName.dbId}"
    }

    private fun tissueUrl(): String {
        return "${analysisUrl()}/identifiers/url/projection?null&interactors=false"
    }

    private fun tissuePayload(tissues: List<TissueName>): String {
        val tissueIds = sortedUniqueTissueIds(tissues)
        return "https://127.0.0.1/ExperimentDigester/experiments/1/sample?included=${tissueIds}&omitNulls=true"
    }

    private fun sortedUniqueTissueIds(tissues: List<TissueName>): String {
        return tissues.map { it.tissueId }.distinct().sorted().joinToString(",")
    }
}

private fun writeToDisk(fileName: String, content: String) {
    File(fileName).writeText(content)
}

private fun extractToken(jsonBody: String): String {
    val tokenRegex = """"token"\s*:\s*"([^"]+)"""".toRegex()
    val tokenMatch = tokenRegex.find(jsonBody)
    val tokenAlt = tokenMatch?.groupValues?.get(1)
    return tokenAlt ?: throw IllegalArgumentException("Token not found in the response")
}

