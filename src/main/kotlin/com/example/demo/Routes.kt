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
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import java.util.*


@Configuration
class Routes {

  @Bean
  fun routesFun(objectMapper: ObjectMapper) = router {
    (accept(APPLICATION_JSON)).nest {
      GET("/graphql", { req ->
        val queryParameters = queryParametersFromRequest(req)
        serveGraphql(queryParameters)
      })
      POST("/graphql", { req ->
        req.bodyToMono(String::class.java)
          .flatMap { body ->
            val queryParameters = objectMapper.readValue(body, QueryParameters::class.java)
            serveGraphql(queryParameters)
          }
      })
      GET("/", { ok().body(fromResource(ClassPathResource("/graphiql.html"))) })
    }
  }
}

fun queryParametersFromRequest(req: ServerRequest): QueryParameters {
  return QueryParameters(
    query = req.queryParam("query").get(),
    operationName = req.queryParam("operationName").orElseGet { null },
    variables = getJsonAsMap(req.queryParam("variables").orElseGet { null })
  )
}

val schema = buildSchema()

fun serveGraphql(queryParameters: QueryParameters): Mono<ServerResponse> {
  val executionInput = newExecutionInput()
    .query(queryParameters.query)
    .operationName(queryParameters.operationName)
    .variables(queryParameters.variables)

  val graphQL = GraphQL
    .newGraphQL(schema)
    .build()

  val executionResult = graphQL.executeAsync(executionInput)
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
