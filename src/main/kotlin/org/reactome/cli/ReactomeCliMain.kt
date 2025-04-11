package org.reactome.cli

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.Callable
import mu.KotlinLogging
import picocli.CommandLine
import picocli.CommandLine.Command

@Command(mixinStandardHelpOptions = true)
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