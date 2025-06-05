package org.reactome.cli

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File

private const val CLI_USER_AGENT = "Mozilla/5.0 (compatible; Reactome CLI/1.0)"

class ReactomeCli(
    private val httpClient: HttpClient,
    private val reactomeUrl: String,
) {

    fun analyseGenes(identifiersFile: String, projectToHuman: Boolean, includeInteractions: Boolean, options: CommonOptions) {
        val responseBody = analyzeGenes(identifiersFile, projectToHuman, includeInteractions)
        val token = extractToken(responseBody)
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

        if (options.pdfReportFile != null) {
            downloadReportPdf(token, filename = options.pdfReportFile.toString())
        }
    }

    private fun analyzeGenes(inputFile: String, projectToHuman: Boolean, includeInteractors: Boolean): String {
        return runBlocking {
            val url = pagedUrl(identifiersUrl(projectToHuman, includeInteractors), pageSize = 20, page = 1)
            val response = httpClient.post(url) {
                contentType(ContentType.Text.Plain)
                userAgent(CLI_USER_AGENT)
                setBody(File(inputFile).readText())
            }

            if (response.status.value != 200) {
                throw Exception("Gene analysis failed: ${response.status.value} - ${response.body<String>()}")
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
                throw Exception("Species analysis failed: ${response.status.value} - ${response.body<String>()}")
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
                throw Exception("Tissue analysis failed: ${response.status.value} - ${response.body<String>()}")
            }

            response.body()
        }
    }

    private fun downloadEntitiesFound(token: String, resource: ResourceType = ResourceType.TOTAL, filename: String) {
        val url = "${analysisServiceUrl()}/download/${token}/entities/found/${resource}/entities_found.csv"
        val content = getTextContent(url)
        File(filename).writeText(content)
    }

    private fun downloadEntitiesNotFound(token: String, filename: String) {
        val url = "${analysisServiceUrl()}/download/${token}/entities/notfound/entities_not_found.csv"
        val content = getTextContent(url)
        File(filename).writeText(content)
    }

    private fun downloadPathways(token: String, resource: ResourceType = ResourceType.TOTAL, filename: String): String {
        val url = "${analysisServiceUrl()}/download/${token}/pathways/${resource}/pathways.csv"
        val content = getTextContent(url)
        File(filename).writeText(content)
        return content
    }

    private fun downloadResultJson(token: String, filename: String) {
        val url = "${analysisServiceUrl()}/download/${token}/result.json"
        val content = getTextContent(url)
        File(filename).writeText(content)
    }

    private fun downloadReportPdf(token: String, species: String = "Homo%20sapiens", filename: String) {
        val url = "${analysisServiceUrl()}/report/${token}/${species}/report.pdf"
        val content = getBinaryContent(url)
        File(filename).writeBytes(content)
    }

    private suspend fun executeHttpRequest(url: String): HttpResponse {
        val response = httpClient.get(url) {
            userAgent(CLI_USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml,application/pdf;q=0.9,*/*;q=0.8")
            header(HttpHeaders.AcceptEncoding, "gzip, deflate, br")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
        }

        if (response.status != HttpStatusCode.OK) {
            val errorBody = try {
                response.body<String>()
            } catch (e: Exception) {
                "[Could not read error body: ${e.message}]"
            }
            throw Exception("Request failed: $url ${response.status.value} - $errorBody")
        }
        return response
    }

    private fun getTextContent(url: String): String {
        return runBlocking {
            val response = executeHttpRequest(url)
            val charset = response.contentType()?.charset() ?: Charsets.UTF_8
            String(response.body<ByteArray>(), charset)
        }
    }

    private fun getBinaryContent(url: String): ByteArray {
        return runBlocking {
            val response = executeHttpRequest(url)
            response.body<ByteArray>()
        }
    }

    private fun analysisServiceUrl(): String {
        return "$reactomeUrl/AnalysisService"
    }

    private fun pagedUrl(url: String, pageSize: Int, page: Int, joinWith: String = "&"): String {
        return "$url${joinWith}pageSize=$pageSize&page=$page"
    }

    private fun identifiersUrl(projectToHuman: Boolean, includeInteractors: Boolean): String {
        val projection = if (projectToHuman) "projection" else ""
        return "${analysisServiceUrl()}/identifiers/${projection}?null&interactors=$includeInteractors"
    }

    private fun speciesUrl(speciesName: SpeciesName): String {
        return "${analysisServiceUrl()}/species/homoSapiens/${speciesName.dbId}"
    }

    private fun tissueUrl(): String {
        return "${analysisServiceUrl()}/identifiers/url/projection?null&interactors=false"
    }

    private fun tissuePayload(tissues: List<TissueName>): String {
        val tissueIds = sortedUniqueTissueIds(tissues)
        // This 127.0.0.1 url actually passed to the Reactome server, it is not a local URL.
        // The Reactome server uses this URL to fetch tissue samples from itself.
        return "https://127.0.0.1/ExperimentDigester/experiments/1/sample?included=${tissueIds}&omitNulls=true"
    }

    private fun sortedUniqueTissueIds(tissues: List<TissueName>): String {
        return tissues.map { it.tissueId }.distinct().sorted().joinToString(",")
    }
}

fun extractToken(jsonBody: String): String {
    val tokenRegex = """"token"\s*:\s*"([^"]+)"""".toRegex()
    val tokenMatch = tokenRegex.find(jsonBody)
    val token = tokenMatch?.groupValues?.get(1)
    return token ?: throw IllegalArgumentException("Token not found in the response")
}

