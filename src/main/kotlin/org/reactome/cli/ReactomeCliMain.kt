package org.reactome.cli

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import mu.KotlinLogging
import picocli.CommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.Callable

private const val DEFAULT_TIMEOUT_MILLIS = 60_000L
private const val DEFAULT_REACTOME_URL = "https://reactome.org"
private const val SUCCESS_EXIT_CODE = 0
private const val FAILURE_EXIT_CODE = 1

class ReactomeCliMain : Callable<Int> {

    @CommandLine.Option(
        names = ["-u", "--reactome_url"],
        description = ["URL of the Reactome server, e.g. $DEFAULT_REACTOME_URL"],
        required = false
    )
    var reactomeUrl: String = DEFAULT_REACTOME_URL

    @CommandLine.Option(
        names = ["-i", "--identifiers_file"],
        description = ["Input file containing gene/protein identifiers"],
        required = true
    )
    lateinit var identifiersFilePath: String

    @CommandLine.Option(
        names = ["--project_to_human"],
        description = ["Convert all non-human identifiers to their human equivalents (true/false)"],
        required = false
    )
    var projectToHuman: Boolean = true

    @CommandLine.Option(
        names = ["--include_interactors"],
        description = ["Use IntAct interactors to increase the analysis background (true/false)"],
        required = false,
        defaultValue = "false",
        arity = "1"
    )
    var includeInteractors: Boolean = false

    @CommandLine.Option(
        names = ["-o", "--output"],
        description = ["Path to the output file (optional)"],
        required = false
    )
    var outputFile: Path? = null

    @CommandLine.Option(
        names = ["--pathways"],
        description = ["Pathways output csv file (optional)"],
        required = false
    )
    var pathwaysFile: Path? = null

    @CommandLine.Option(
        names = ["--entities_found"],
        description = ["Entities found output csv file (optional)"],
        required = false
    )
    var entitiesFoundFile: Path? = null

    @CommandLine.Option(
        names = ["--entities_not_found"],
        description = ["Entities not found output csv file (optional)"],
        required = false
    )
    var entitiesNotFoundFile: Path? = null

    @CommandLine.Option(
        names = ["--result_json"],
        description = ["Complete analysis results json file (optional)"],
        required = false
    )
    var resultJsonFile: Path? = null

    @CommandLine.Option(
        names = ["--report_pdf"],
        description = ["Report pdf output file (optional)"],
        required = false
    )
    var reportPdfFile: Path? = null

    @CommandLine.Option(
        names = ["--html_report"],
        description = ["HTML report output file (optional)"],
        required = false
    )
    var htmlReportFile: Path? = null

    @CommandLine.Option(
        names = ["-v", "--verbose"],
        description = ["Enable verbose mode"]
    )
    var verbose: Boolean = false

    @CommandLine.Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = ["Display this help message"]
    )
    var helpRequested: Boolean = false

    private val logger = KotlinLogging.logger {}

    override fun call(): Int {
        logger.info { "Starting reactome CLI" }
        return try {
            val result = ReactomeCli(httpClient(), reactomeUrl, identifiersFilePath, includeInteractors, pathwaysFile,
                entitiesFoundFile, entitiesNotFoundFile, resultJsonFile, reportPdfFile, htmlReportFile).execute()
            outputFile?.let {
                Files.writeString(it, result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            } ?: println(result)
            SUCCESS_EXIT_CODE
        } catch (e: Exception) {
            logger.error("Could not complete CLI execution", e)
            FAILURE_EXIT_CODE
        }
    }

    private fun httpClient(): HttpClient {
        return HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = DEFAULT_TIMEOUT_MILLIS
                connectTimeoutMillis = DEFAULT_TIMEOUT_MILLIS
                socketTimeoutMillis = DEFAULT_TIMEOUT_MILLIS
            }
        }
    }
}

fun main(args: Array<String>): Unit = kotlin.system.exitProcess(CommandLine(ReactomeCliMain()).execute(*args))