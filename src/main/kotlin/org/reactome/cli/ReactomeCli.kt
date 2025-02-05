package org.reactome.cli

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.io.File
import kotlinx.coroutines.runBlocking

class ReactomeCli(private val httpClient: HttpClient, private val reactomeUrl: String, private val proteinFilePath: String) {
    fun execute(): String {
        return runBlocking {
            httpClient.post("$reactomeUrl/AnalysisService/identifiers/projection?null&interactors=false&pageSize=20&page=1") {
                contentType(ContentType.Text.Plain)
                setBody(File(proteinFilePath).readText())
            }.body<String>()
        }
    }
}