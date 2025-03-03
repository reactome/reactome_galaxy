package org.reactome.cli

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import mu.KotlinLogging
import picocli.CommandLine
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.Callable

class ReactomeCliMain : Callable<Int> {

    @CommandLine.Option(names = ["--reactome_url"])
    lateinit var reactomeUrl: String

    @CommandLine.Option(names = ["--protein_file_path"])
    lateinit var proteinFilePath: String

    @CommandLine.Option(
        names = ["-o", "--output"],
        description = ["Path to the output file (optional)"],
        required = false
    )
    var outputFilePath: Path? = null

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
            val result = ReactomeCli(HttpClient(CIO), reactomeUrl, proteinFilePath).execute()
            outputFilePath?.let {
                Files.writeString(it, result, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            } ?: println(result)
            0
        } catch (e: Exception) {
            logger.error("Could not complete CLI execution", e)
            1
        }
    }
}

fun main(args: Array<String>): Unit = kotlin.system.exitProcess(CommandLine(ReactomeCliMain()).execute(*args))