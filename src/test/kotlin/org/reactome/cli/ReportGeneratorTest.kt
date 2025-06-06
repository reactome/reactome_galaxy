package org.reactome.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

private val PATHWAYS_FILE = "${System.getProperty("user.dir")}/src/test/resources/genes_output/pathways.csv"
private val EXPECTED_HTML_REPORT_FILE = "${System.getProperty("user.dir")}/src/test/resources/genes_output/report.html"
private val TOKEN = "MjAyNTA2MDUxMzE5NThfNTUyMDg%3D"
private val EXPECTED_HEADERS = listOf(
    "Pathway identifier",
    "Pathway name",
    "#Entities found",
    "#Entities total",
    "Entities ratio",
    "Entities pValue",
    "Entities FDR",
    "#Reactions found",
    "#Reactions total",
    "Reactions ratio",
    "Species identifier",
    "Species name",
    "Submitted entities found",
    "Mapped entities",
    "Found reaction identifiers"
)

class ReportGeneratorTest {
    @Test
    fun `should generate HTML report`() {
        val reportGenerator = ReportGenerator("https://reactome.org")
        val data = File(PATHWAYS_FILE).readText(Charsets.UTF_8)
        val csvData = readCsvData(data)
        val generatedReport = reportGenerator.generateReportFromCsvData(csvData, TOKEN)
        val expectedReport = File(EXPECTED_HTML_REPORT_FILE).readText(Charsets.UTF_8)
        assertThat(generatedReport).isEqualTo(expectedReport)
    }

    @Test
    fun `Should parse pathways CSV data`() {
        val data = File(PATHWAYS_FILE).readText(Charsets.UTF_8)
        val csvData = readCsvData(data)
        assertThat(csvData.headers).isEqualTo(EXPECTED_HEADERS)
        assertThat(csvData.rows).hasSize(12)
        csvData.rows.forEach { row ->
            assertThat(row).hasSize(EXPECTED_HEADERS.size)
        }

        val firstRow = csvData.rows.first()
        assertThat(firstRow).containsEntry("Pathway identifier", "R-HSA-8963896")
        assertThat(firstRow).containsEntry("Pathway name", "HDL assembly")
        assertThat(firstRow).containsEntry("#Entities total", "18")
    }
}