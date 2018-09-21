package com.oshai

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.mysql.MySQLConnection
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.pipeline.PipelineContext
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.future.await
import mu.KotlinLogging


fun main(args: Array<String>) {
  val server = embeddedServer(Netty, port = 8080) {
    install(ContentNegotiation) {
      gson {
        setPrettyPrinting()
      }

      routing {
        get("/") {
          logger.info { "handling mysql request" }
          handleMysqlRequest("select 0")
        }
      }
    }
  }
  println("STARTING")
  connection.connect().get()
  try {
    server.start(wait = true)
  } finally {
    println("DISCO")
    connection.disconnect().get()
  }
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


private suspend fun PipelineContext<Unit, ApplicationCall>
        .handleMysqlRequest(query: String) {
    val queryResult = connection.sendPreparedStatementAwait(query = query)
    call.respond(queryResult.rows!![0][0].toString())
}

private suspend fun Connection.sendPreparedStatementAwait(query: String, values: List<Any> = emptyList()): QueryResult =
        this.sendPreparedStatement(query, values).await()
