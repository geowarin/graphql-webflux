package com.example.demo

import com.fasterxml.jackson.databind.type.MapType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import graphql.ExecutionInput.newExecutionInput
import graphql.GraphQL
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters.fromResource
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.*
import java.net.URLDecoder
import java.util.*

val GraphQLMediaType = MediaType.parseMediaType("application/GraphQL")
val MapTypeRef: MapType =
  TypeFactory.defaultInstance().constructMapType(HashMap::class.java, String::class.java, Any::class.java)

@Configuration
class Routes {

  @Bean
  fun routesFun() = router {
    GET("/", { ok().body(fromResource(ClassPathResource("/graphiql.html"))) })
    (POST("/graphql") or GET("/graphql")).invoke { req: ServerRequest ->
      getParams(req).flatMap { serveGraphql(it) }
    }
  }
}

fun getParams(req: ServerRequest): Mono<QueryParameters> {
  return when {
    req.method() == HttpMethod.GET -> parseGetRequest(req)
    else -> parsePostRequest(req)
  }
}

fun parseGetRequest(req: ServerRequest) = just(queryParametersFromRequest(req))

fun parsePostRequest(req: ServerRequest) = when {
  req.queryParam("query").isPresent -> just(queryParametersFromRequest(req))
  req.contentTypeIs(GraphQLMediaType) -> req.bodyToMono<String>().flatMap { body ->
    just(QueryParameters(query = body))
  }
  else -> req.bodyToMono<String>().flatMap { body ->
    val postParams = jacksonObjectMapper().readValue(body, QueryParameters::class.java)
    just(postParams)
  }
}

fun ServerRequest.contentTypeIs(mediaType: MediaType)
  = this.headers().contentType().filter { it.isCompatibleWith(mediaType) }.isPresent

fun queryParametersFromRequest(req: ServerRequest): QueryParameters {
  return QueryParameters(
    query = req.queryParam("query").get(),
    operationName = req.queryParam("operationName").orElseGet { null },
    variables = getVariables(req)
  )
}

fun getVariables(req: ServerRequest): Map<String, Any>? {
  return req.queryParam("variables")
    .map { URLDecoder.decode(it, "UTF-8") }
    .map { getJsonAsMap(it) }
    .orElseGet { null }
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
  return ok().body(fromFuture(executionResult))
}

data class QueryParameters(
  val query: String,
  val operationName: String? = null,
  val variables: Map<String, Any>? = null
)

fun getJsonAsMap(variables: String?): Map<String, Any>? {
  return jacksonObjectMapper().readValue(variables, MapTypeRef)
}
