package com.example.demo

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import graphql.ExecutionInput.newExecutionInput
import graphql.GraphQL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.BodyInserters.fromResource
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import java.util.*


@Configuration
class Routes {

  @Bean
  fun routesFun() = router {
    (accept(APPLICATION_JSON)).nest {
      GET("/graphql", { req ->

        val queryParameters = QueryParameters(
          query = req.queryParam("query").get(),
          operationName = req.queryParam("operationName").get(),
          variables = getJsonAsMap(req.queryParam("variables").get())
        )
        serveGraphql(queryParameters)
      })
      POST("/graphql", { req ->

        req.bodyToMono(String::class.java)
          .flatMap { body ->
            val bodyMap = getJsonAsMap(body)
            val queryParameters = QueryParameters(
              query = bodyMap.getValue("query") as String,
              operationName = bodyMap["operationName"] as String?,
              variables = bodyMap["variables"] as Map<String, Any>?
            )
            serveGraphql(queryParameters)
          }

      })
      GET("/", { ok().body(fromResource(ClassPathResource("/graphiql.html"))) })
    }
  }
}


fun serveGraphql(queryParameters: QueryParameters): Mono<ServerResponse> {
  val executionInput = newExecutionInput()
    .query(queryParameters.query)
    .operationName(queryParameters.operationName)
    .variables(queryParameters.variables)

  val schema = buildSchema()

  val graphQL = GraphQL
    .newGraphQL(schema)
    .build()
  val executionResult = graphQL.executeAsync(executionInput.build())

  return ok().body(Mono.fromFuture(executionResult))
}

data class QueryParameters(
  val query: String,
  val operationName: String?,
  val variables: Map<String, Any>?
)

fun getJsonAsMap(variables: String): Map<String, Any> {
  val typeRef =
    TypeFactory.defaultInstance().constructMapType(HashMap::class.java, String::class.java, Any::class.java)
  return ObjectMapper().readValue(variables, typeRef)
}
