package com.example.demo

import graphql.ExecutionInput.newExecutionInput
import graphql.GraphQL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.fromFuture
import reactor.core.publisher.Mono.just
import java.net.URLDecoder

val GraphQLMediaType = MediaType.parseMediaType("application/GraphQL")
val schema = buildSchema()

@Configuration
class Routes {

  @Bean
  fun routesFun() = router {
    GET("/", serveStatic(ClassPathResource("/graphiql.html")))
    (POST("/graphql") or GET("/graphql")).invoke { req: ServerRequest ->
      getParams(req).flatMap { serveGraphql(it) }
    }
  }
}

fun serveGraphql(queryParameters: QueryParameters): Mono<ServerResponse> {
  val executionInput = newExecutionInput()
    .query(queryParameters.query)
    .operationName(queryParameters.operationName)
    .variables(queryParameters.variables)

  val graphQL = GraphQL
    .newGraphQL(schema)
    .build()

  val executionResult = graphQL.executeAsync(executionInput)
  return ok().body(fromFuture(executionResult))
}

fun getParams(req: ServerRequest): Mono<QueryParameters> {
  return when {
    req.method() == HttpMethod.GET -> parseGetRequest(req)
    else -> parsePostRequest(req)
  }
}

fun parseGetRequest(req: ServerRequest) = queryParametersFromRequest(req)

fun parsePostRequest(req: ServerRequest) = when {
  req.queryParam("query").isPresent -> queryParametersFromRequest(req)
  req.contentTypeIs(GraphQLMediaType) -> req.withBody { QueryParameters(query = it) }
  else -> req.withBody { toJson<QueryParameters>(it) }
}

fun queryParametersFromRequest(req: ServerRequest): Mono<QueryParameters> {
  return just(
    QueryParameters(
      query = req.queryParam("query").get(),
      operationName = req.queryParam("operationName").orElseGet { null },
      variables = getVariables(req)
    )
  )
}

fun getVariables(req: ServerRequest): Map<String, Any>? {
  return req.queryParam("variables")
    .map { URLDecoder.decode(it, "UTF-8") }
    .map { toJsonMap(it) }
    .orElseGet { null }
}

data class QueryParameters(
  val query: String,
  val operationName: String? = null,
  val variables: Map<String, Any>? = null
)
