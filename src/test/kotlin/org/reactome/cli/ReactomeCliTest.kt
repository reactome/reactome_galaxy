package org.reactome.cli

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

private const val REACTOME_TEST_URL = "http://example.com"

private val RESOURCE_DIR = System.getProperty("user.dir") + "/src/test/resources/"
private val UNIPROT_INPUT_FILE = "$RESOURCE_DIR/uniprot.txt"

private const val PATHWAY_ID = "R-HSA-ID"
private val PATHWAYS_CSV_RESPONSE_CONTENT =
    """
    Pathway identifier,Pathway name,other column
    $PATHWAY_ID,Pathway Name,other column value
    """.trimIndent()

private const val ENTITIES_FOUND_CSV_RESPONSE_CONTENT = "Entities found CSV response content for testing purposes"
private const val ENTITIES_NOT_FOUND_CSV_RESPONSE_CONTENT = "Entities not found CSV response content for testing purposes"
private const val FULL_ANALYSIS_JSON_RESPONSE_CONTENT = """{ "summary": "JSON response body for testing purposes" }"""
private const val PDF_RESPONSE_CONTENT = "PDF response content for testing purposes"

private const val TOKEN = "response-token"
private const val ANALYSIS_INTERMEDIATE_JSON_RESPONSE_CONTENT = """{"summary": { "token": "$TOKEN" }}"""

private data class TestOutputs(
    val pathwaysOutput: File,
    val pdfOutput: File,
    val htmlOutput: File,
    val entitiesFoundOutput: File,
    val entitiesNotFoundOutput: File,
    val options: CommonOptions
)

private fun createTempOutputsAndOptions(tempDir: Path): TestOutputs {
    val pathwaysOutput = tempDir.resolve("pathways.csv").toFile()
    val pdfOutput = tempDir.resolve("report.pdf").toFile()
    val htmlOutput = tempDir.resolve("pathway_links.html").toFile()
    val entitiesFoundOutput = tempDir.resolve("entities_found.csv").toFile()
    val entitiesNotFoundOutput = tempDir.resolve("entities_not_found.csv").toFile()

    val options = CommonOptions().apply {
        pathwaysFile = pathwaysOutput.toPath()
        pdfReportFile = pdfOutput.toPath()
        htmlReportFile = htmlOutput.toPath()
        entitiesFoundFile = entitiesFoundOutput.toPath()
        entitiesNotFoundFile = entitiesNotFoundOutput.toPath()
    }

    return TestOutputs(
        pathwaysOutput,
        pdfOutput,
        htmlOutput,
        entitiesFoundOutput,
        entitiesNotFoundOutput,
        options
    )
}

class ReactomeCliTest {

    @Test
    fun `Should execute gene analysis and write all output files`(@TempDir tempDir: Path) {
        val testOutputs = createTempOutputsAndOptions(tempDir)

        val cli = ReactomeCli(testHttpClient(), REACTOME_TEST_URL)
        cli.analyseGenes(UNIPROT_INPUT_FILE, projectToHuman = true, includeInteractions = true, testOutputs.options)

        validateResponseContents(testOutputs)
    }

    @Test
    fun `Should execute tissue analysis and write all output files`(@TempDir tempDir: Path) {
        val testOutputs = createTempOutputsAndOptions(tempDir)

        val cli = ReactomeCli(testHttpClient(), REACTOME_TEST_URL)
        cli.analyseTissues(listOf(TissueName.HEART_MUSCLE), testOutputs.options)

        validateResponseContents(testOutputs)
    }

    @Test
    fun `Should execute species analysis and write all output files`(@TempDir tempDir: Path) {
        val testOutputs = createTempOutputsAndOptions(tempDir)

        val cli = ReactomeCli(testHttpClient(), REACTOME_TEST_URL)
        cli.analyseSpecies(SpeciesName.HUMAN, testOutputs.options)

        validateResponseContents(testOutputs)
    }

    private fun validateResponseContents(testOutputs: TestOutputs) {
        assertThat(testOutputs.pathwaysOutput.readText()).isEqualTo(PATHWAYS_CSV_RESPONSE_CONTENT)
        assertThat(testOutputs.pdfOutput.readText()).isEqualTo(PDF_RESPONSE_CONTENT)

        val htmlResultBody = testOutputs.htmlOutput.readText()
        assertThat(htmlResultBody).contains("<title>Reactome Pathway Links</title>")
        assertThat(htmlResultBody).contains(PATHWAY_ID)

        assertThat(testOutputs.entitiesFoundOutput.readText()).isEqualTo(ENTITIES_FOUND_CSV_RESPONSE_CONTENT)
        assertThat(testOutputs.entitiesNotFoundOutput.readText()).isEqualTo(ENTITIES_NOT_FOUND_CSV_RESPONSE_CONTENT)
    }

    @Test
    fun `Should throw exception on http error`() {
        val cli = ReactomeCli(testHttpClient(), "WRONG_URL")
        assertThatThrownBy { cli.analyseGenes(UNIPROT_INPUT_FILE, projectToHuman = true, includeInteractions = true, CommonOptions()) }
            .isInstanceOf(Exception::class.java)
            .hasMessageContaining("analysis failed")
    }

    @Test
    fun `Should extract token from JSON response`() {
        val json = """{"summary": { "token": "test-token" }}"""
        val token = extractToken(json)
        assertThat(token).isEqualTo("test-token")
    }

    private fun testHttpClient(): HttpClient {
        val mockEngine = MockEngine { request ->
            if (request.method == HttpMethod.Post && request.url.encodedPath == "/AnalysisService/identifiers/projection") {
                respond(
                    content = ANALYSIS_INTERMEDIATE_JSON_RESPONSE_CONTENT,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type" to listOf(ContentType.Application.Json.toString()))
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/download/${TOKEN}/result.json")) {
                respond(
                    content = FULL_ANALYSIS_JSON_RESPONSE_CONTENT,
                    status = HttpStatusCode.OK,
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/download/${TOKEN}/entities/found/TOTAL/entities_found.csv")) {
                respond(
                    content = ENTITIES_FOUND_CSV_RESPONSE_CONTENT,
                    status = HttpStatusCode.OK,
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/download/${TOKEN}/entities/notfound/entities_not_found.csv")) {
                respond(
                    content = ENTITIES_NOT_FOUND_CSV_RESPONSE_CONTENT,
                    status = HttpStatusCode.OK,
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/download/${TOKEN}/pathways/TOTAL/pathways.csv")) {
                respond(
                    content = PATHWAYS_CSV_RESPONSE_CONTENT,
                    status = HttpStatusCode.OK,
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/report/${TOKEN}/Homo%20sapiens/report.pdf")) {
                respond(
                    content = PDF_RESPONSE_CONTENT,
                    status = HttpStatusCode.OK,
                )
            } else if (request.method == HttpMethod.Post && request.url.encodedPath.startsWith("/AnalysisService/identifiers/url/projection")) {
                respond(
                    content = ANALYSIS_INTERMEDIATE_JSON_RESPONSE_CONTENT,
                    status = HttpStatusCode.OK,
                )
            } else if (request.method == HttpMethod.Get && request.url.encodedPath.startsWith("/AnalysisService/species/homoSapiens/${SpeciesName.HUMAN.dbId}")) {
                respond(
                    content = ANALYSIS_INTERMEDIATE_JSON_RESPONSE_CONTENT,
                    status = HttpStatusCode.OK,
                )
            } else {
                respondError(HttpStatusCode.NotFound, "Not found: ${request.method} ${request.url.encodedPath}")
            }
        }

        return HttpClient(mockEngine)
    }
}