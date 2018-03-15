package com.geowarin.graphql

import graphql.*
import graphql.ExecutionInput.*
import org.springframework.context.annotation.*
import org.springframework.core.io.ClassPathResource
import org.springframework.http.*
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.*
import java.net.URLDecoder

val GraphQLMediaType = MediaType.parseMediaType("application/GraphQL")
val schema = buildSchema()

@Configuration
class Routes {

  @Bean
  fun routesFun() = router {
    GET("/", serveStatic(ClassPathResource("/graphiql.html")))
    (POST("/graphql") or GET("/graphql")).invoke { req: ServerRequest ->
      getGraphQLParameters(req)
        .flatMap { executeGraphQLQuery(it) }
        .flatMap { ok().syncBody(it) }
        .switchIfEmpty(badRequest().build())
    }
  }
}

fun executeGraphQLQuery(graphQLParameters: GraphQLParameters): Mono<ExecutionResult> {
  val executionInput = newExecutionInput()
    .query(graphQLParameters.query)
    .operationName(graphQLParameters.operationName)
    .variables(graphQLParameters.variables)

  val graphQL = GraphQL
    .newGraphQL(schema)
    .build()

  return fromFuture(graphQL.executeAsync(executionInput))
}

fun getGraphQLParameters(req: ServerRequest): Mono<GraphQLParameters> {
  return when {
    req.queryParam("query").isPresent -> graphQLParametersFromRequestParameters(req)
    req.method() == HttpMethod.POST -> parsePostRequest(req)
    else -> empty()
  }
}

fun parsePostRequest(req: ServerRequest) = when {
  req.contentTypeIs(GraphQLMediaType) -> req.withBody { GraphQLParameters(query = it) }
  else -> req.withBody { toJson<GraphQLParameters>(it) }
}

fun graphQLParametersFromRequestParameters(req: ServerRequest) =
  just(
    GraphQLParameters(
      query = req.queryParam("query").get(),
      operationName = req.queryParam("operationName").orElseGet { null },
      variables = getVariables(req)
    )
  )

fun getVariables(req: ServerRequest): Map<String, Any>? {
  return req.queryParam("variables")
    .map { URLDecoder.decode(it, "UTF-8") }
    .map { toJsonMap(it) }
    .orElseGet { null }
}

data class GraphQLParameters(
  val query: String,
  val operationName: String? = null,
  val variables: Map<String, Any>? = null
)
