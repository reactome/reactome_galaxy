package org.reactome.cli

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import mu.KotlinLogging
import picocli.CommandLine
import java.util.concurrent.Callable

class ReactomeCliMain : Callable<Int> {

    @CommandLine.Option(names = ["--reactome_url"])
    lateinit var reactomeUrl: String

    @CommandLine.Option(names = ["--protein_file_path"])
    lateinit var proteinFilePath: String

    private val logger = KotlinLogging.logger {}

    override fun call(): Int {
        logger.info { "Starting reactome CLI" }
        try {
            println(ReactomeCli(HttpClient(CIO), reactomeUrl, proteinFilePath).execute())
        } catch (e: Exception) {
            logger.error("Could not complete CLI execution", e)
        }
        return 0
    }
}

fun main(args: Array<String>): Unit = kotlin.system.exitProcess(CommandLine(ReactomeCliMain()).execute(*args))