package org.reactome.cli

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import mu.KotlinLogging
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin
import picocli.CommandLine.Option
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.system.exitProcess

private const val DEFAULT_TIMEOUT_MILLIS = 60_000L
private const val DEFAULT_REACTOME_URL = "https://reactome.org"
private const val SUCCESS_EXIT_CODE = 0
private const val FAILURE_EXIT_CODE = 1

@Command(
    name = "reactome-cli",
    description = ["A CLI tool for gene analysis and species processing"],
    subcommands = [GeneCommand::class, SpeciesCommand::class, TissuesCommand::class],
    mixinStandardHelpOptions = true
)
class ReactomeCliMain : Runnable {
    override fun run() {
        println("Please specify a subcommand: genes, species or tissues")
    }
}

@Command(name = "genes", description = ["Analyze genes with provided options"])
class GeneCommand : Callable<Int> {

    @Option(
        names = ["-i", "--identifiers_file"],
        description = ["Input file containing gene/protein identifiers"],
        required = true
    )
    lateinit var identifiersFilePath: String

    @Option(
        names = ["--project_to_human"],
        description = ["Convert all non-human identifiers to their human equivalents (true/false)"],
        required = false,
        defaultValue = "true",
        arity = "1"
    )
    var projectToHuman: Boolean = true

    @Option(
        names = ["--include_interactors"],
        description = ["Use IntAct interactors to increase the analysis background (true/false)"],
        required = false,
        defaultValue = "false",
        arity = "1"
    )
    var includeInteractors: Boolean = false

    @Mixin
    lateinit var commonOptions: CommonOptions

    @Option(
        names = ["-v", "--verbose"],
        description = ["Enable verbose mode"]
    )
    var verbose: Boolean = false

    private val logger = KotlinLogging.logger {}

    override fun call(): Int {
        logger.info { "Starting reactome CLI" }
        return try {
            val reactomeCli = ReactomeCli(httpClient(), commonOptions.reactomeUrl)
            reactomeCli.analyseGenes(identifiersFilePath, projectToHuman, includeInteractors, commonOptions)
            SUCCESS_EXIT_CODE
        } catch (e: Exception) {
            logger.error("Could not complete CLI execution", e)
            FAILURE_EXIT_CODE
        }
    }
}

@Command(name = "species", description = ["Analyse species"])
class SpeciesCommand : Callable<Int> {

    @Option(
        names = ["--speciesName"],
        description = ["Name of the species"],
        required = true
    )
    lateinit var speciesName: String

    @Mixin
    lateinit var commonOptions: CommonOptions

    private val logger = KotlinLogging.logger {}

    override fun call(): Int {
        val species = SpeciesName.lookup(speciesName)
        if (species == null) {
            println("Species not found: $speciesName")
            return FAILURE_EXIT_CODE
        }
        logger.info { "Starting reactome CLI" }
        return try {
            val reactomeCli = ReactomeCli(httpClient(), commonOptions.reactomeUrl)
            reactomeCli.analyseSpecies(species, commonOptions)
            SUCCESS_EXIT_CODE
        } catch (e: Exception) {
            logger.error("Could not complete CLI execution", e)
            FAILURE_EXIT_CODE
        }
    }
}

@Command(name = "tissues", description = ["Analyse tissues"])
class TissuesCommand : Callable<Int> {

    @Option(
        names = ["--tissues"],
        description = ["List of tissues, e.g. \"Adrenal gland, Kidney\""],
        required = true
    )
    lateinit var tissues: String

    @Mixin
    lateinit var commonOptions: CommonOptions

    private val logger = KotlinLogging.logger {}

    override fun call(): Int {
        val parititionedTissues = tissues.split(",")
            .map { it -> it to TissueName.lookup(it.trim()) }
            .partition { it.second != null }
        val recognizedTissues = parititionedTissues.first.mapNotNull { it.second }
        val unrecognizedTissues = parititionedTissues.second.map { it.first }

        if (unrecognizedTissues.isNotEmpty()) {
            println("Tissues not found: ${unrecognizedTissues.joinToString(",")}")
            return FAILURE_EXIT_CODE
        }

        logger.info { "Starting reactome CLI" }
        return try {
            val reactomeCli = ReactomeCli(httpClient(), commonOptions.reactomeUrl)
            reactomeCli.analyseTissues(recognizedTissues, commonOptions)
            SUCCESS_EXIT_CODE
        } catch (e: Exception) {
            logger.error("Could not complete CLI execution", e)
            FAILURE_EXIT_CODE
        }
    }
}

class CommonOptions {

    @Option(
        names = ["-u", "--reactome_url"],
        description = ["URL of the Reactome server, e.g. $DEFAULT_REACTOME_URL"],
        required = false
    )
    var reactomeUrl: String = DEFAULT_REACTOME_URL

    @Option(
        names = ["--pdf_report"],
        description = ["PDF report output file (optional)"]
    )
    var pdfReportFile: Path? = null

    @Option(
        names = ["--html_report"],
        description = ["HTML report output file (optional)"]
    )
    var htmlReportFile: Path? = null

    @Option(
        names = ["--pathways"],
        description = ["Pathways output csv file (optional)"],
        required = false
    )
    var pathwaysFile: Path? = null

    @Option(
        names = ["--entities_found"],
        description = ["Entities found output csv file (optional)"],
        required = false
    )
    var entitiesFoundFile: Path? = null

    @Option(
        names = ["--entities_not_found"],
        description = ["Entities not found output csv file (optional)"],
        required = false
    )
    var entitiesNotFoundFile: Path? = null

    @Option(
        names = ["--result_json"], // TODO inconsistently named?
        description = ["Complete analysis results json file (optional)"],
        required = false
    )
    var resultJsonFile: Path? = null
}

fun main(args: Array<String>) {
    val exitCode = CommandLine(ReactomeCliMain()).execute(*args)
    exitProcess(exitCode)
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