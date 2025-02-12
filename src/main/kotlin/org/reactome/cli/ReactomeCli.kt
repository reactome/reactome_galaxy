package org.reactome.cli

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.io.StringWriter

class ReactomeCli(private val httpClient: HttpClient, private val reactomeUrl: String, private val proteinFilePath: String) {
    fun execute(): String {
        val responseBody = analyzeUniprot()
        val analysisResponse = parseResponse(responseBody)
        val reportGenerator = ReportGenerator()

        val stringWriter = StringWriter()
        reportGenerator.writeCsv(analysisResponse, stringWriter)
        return stringWriter.toString()
    }

    private fun analyzeUniprot(): String {
        return runBlocking {
            val response = httpClient.post(pagedUrl(analysisUrl(), pageSize = 20, page = 1)) {
                contentType(ContentType.Text.Plain)
                setBody(File(proteinFilePath).readText())
            }

            if (response.status.value != 200) {
                throw Exception("Failed to submit gene list: ${response.status.value} - ${response.body<String>()}")
            }

            response.body()
        }
    }

    private fun pagedUrl(url: String, pageSize: Int, page: Int): String {
        return "$url&pageSize=$pageSize&page=$page"
    }

    private fun analysisUrl(): String {
        return "$reactomeUrl/AnalysisService/identifiers/projection?null&interactors=false"
    }

    private fun parseResponse(response: String): AnalysisResponse {
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString<AnalysisResponse>(response)
    }
}