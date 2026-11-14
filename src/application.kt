package com.oshai

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.ConnectionPoolConfiguration
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.gson.gson
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.future.await
import java.util.concurrent.TimeUnit

fun main() {
    val server = embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
        routing {
            get("/") {
                logger.info { "handling mysql request" }
                call.handleMysqlRequest("select 0")
            }
        }
    }
    println("STARTING")
    connectionPool.connect().get()
    try {
        server.start(wait = true)
    } finally {
        println("DISCO")
        connectionPool.disconnect().get()
    }
}

private val logger = KotlinLogging.logger {}

val poolConfiguration = ConnectionPoolConfiguration(
    host = "localhost",
    port = 33306,
    database = "mysql_async_tests",
    username = "mysql_async",
    password = "root",
    maxActiveConnections = 100,
    maxIdleTime = TimeUnit.MINUTES.toMillis(15),
    maxPendingQueries = 10_000,
    connectionValidationInterval = TimeUnit.SECONDS.toMillis(30),
)
val connectionPool = MySQLConnectionBuilder.createConnectionPool(poolConfiguration)

private suspend fun ApplicationCall.handleMysqlRequest(query: String) {
    val queryResult = connectionPool.sendPreparedStatementAwait(query = query)
    respond(queryResult.rows[0][0].toString())
}

private suspend fun Connection.sendPreparedStatementAwait(
    query: String,
    values: List<Any?> = emptyList(),
): QueryResult = this.sendPreparedStatement(query, values).await()
