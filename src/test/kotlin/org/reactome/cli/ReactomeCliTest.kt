package org.reactome.cli

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


private const val REACTOME_TEST_URL = "http://example.com"
private val UNITPROT_INPUT_FILE = "${System.getProperty("user.dir")}/src/test/resources/uniprot.txt"
private val JSON_RESPONSE_FILE = "${System.getProperty("user.dir")}/src/test/resources/analysis_response.json"
private val JSON_RESPONSE = java.io.File(JSON_RESPONSE_FILE).readText(Charsets.UTF_8)
private const val TOKEN = "MjAyNTAyMTAxMzMyMjFfOTk%3D"

class ReactomeCliTest {

    @Disabled("need to create new output from pathways file, mabye")
    @Test
    fun `Should return CSV for protein analysis`() {
        val cli = reactomeCli()
        val csvBody = cli.analyseGenes(UNITPROT_INPUT_FILE, includeInteractions = true, CommonOptions())

        val expectedCsvBody = """
            "Pathway name","#Entities found","#Entities total","Entities ratio","Entities pValue","Entities FDR","#Reactions found","#Reactions total","Reactions ratio","Species identifier","Species name"
            "Signaling by FGFR in disease","10","82","0.005199746354","3.09E-06","0.002737723912","47","99","0.006487124042","9606","Homo sapiens"
            "Signaling by Receptor Tyrosine Kinases","29","634","0.040202916931","4.93E-06","0.002737723912","180","759","0.049734617653","9606","Homo sapiens"
        """.trimIndent() + "\n"

        assertThat(csvBody).isEqualTo(expectedCsvBody)
    }

    @Test
    fun `Should throw exception on http error`() {
        val cli = ReactomeCli(testHttpClient(JSON_RESPONSE), "WRONG_URL")
        assertThatThrownBy { cli.analyseGenes(UNITPROT_INPUT_FILE, includeInteractions = true, CommonOptions()) }
            .isInstanceOf(Exception::class.java)
            .hasMessage("Failed to submit gene list: 404 - Not Found")
    }

    private fun testHttpClient(data: String): HttpClient {
        val mockEngine = MockEngine { request ->
            if (request.method == HttpMethod.Post && request.url.encodedPath == "/AnalysisService/identifiers/projection") {
                respond(
                    content = data,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/download/${TOKEN}/result.json")) {
                respond(
                    content = "abcdef",
                    status = HttpStatusCode.OK,
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/download/${TOKEN}/entities/found/TOTAL/entities_found.csv")) {
                respond(
                    content = "abcdef",
                    status = HttpStatusCode.OK,
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/download/${TOKEN}/entities/notfound/entities_not_found.csv")) {
                respond(
                    content = "abcdef",
                    status = HttpStatusCode.OK,
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/download/${TOKEN}/pathways/TOTAL/pathways.csv")) {
                respond(
                    content = "abcdef",
                    status = HttpStatusCode.OK,
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/report/${TOKEN}/Homo%20sapiens/report.pdf")) {
                respond(
                    content = "abcdef",
                    status = HttpStatusCode.OK,
                )
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }

        return HttpClient(mockEngine)
    }

    private fun reactomeCli(): ReactomeCli {
        return ReactomeCli(testHttpClient(JSON_RESPONSE), REACTOME_TEST_URL)
    }
}