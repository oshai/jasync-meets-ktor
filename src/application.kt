package com.oshai

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.mysql.MySQLConnection
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.pipeline.PipelineContext
import kotlinx.coroutines.future.await
import mu.KotlinLogging


fun main(args: Array<String>): Unit {

    io.ktor.server.netty.DevelopmentEngine.main(args)

}

private val logger = KotlinLogging.logger {}

val connection: Connection = MySQLConnection(
        Configuration(
                username = "username",
                password = "password",
                host = "localhost",
                port = 3306,
                database = "schema"
        )
)

@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    install(ContentNegotiation) {
        gson {
        }
    }

    environment.monitor.subscribe(ApplicationStarting) {
        println("STARTING")
        connection.connect().get()
    }

    environment.monitor.subscribe(ApplicationStopping) { application: Application ->
        println("DISCO")
        connection.disconnect().get()
    }

    routing {
//        get("/") {
//            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
//        }

        get("/") {
            logger.info { "handling mysql request" }
            handleMysqlRequest("select 0")
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>
        .handleMysqlRequest(query: String) {
    val queryResult = connection.sendPreparedStatementAwait(query = query)
    call.respond(queryResult.rows!![0][0].toString())
}

suspend fun Connection.sendPreparedStatementAwait(query: String, values: List<Any> = emptyList()): QueryResult =
        this.sendPreparedStatement(query, values).await()
