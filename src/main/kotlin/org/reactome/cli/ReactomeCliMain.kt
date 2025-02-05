package org.reactome.cli

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.util.concurrent.Callable
import mu.KotlinLogging
import picocli.CommandLine

class ReactomeCliMain : Callable<Int> {

    @CommandLine.Option(names = ["--reactome_url"])
    lateinit var reactomeUrl: String

    @CommandLine.Option(names = ["--protein_file_path"])
    lateinit var proteinFilePath: String

    private val logger = KotlinLogging.logger {}

    override fun call(): Int {
        logger.info { "Starting reactome CLI" }
        try {
            ReactomeCli(HttpClient(CIO), reactomeUrl, proteinFilePath).execute()
        } catch (e: Exception) {
            logger.error("Could not complete CLI execution", e)
        }
        return 0
    }
}

fun main(args: Array<String>): Unit = kotlin.system.exitProcess(CommandLine(ReactomeCliMain()).execute(*args))