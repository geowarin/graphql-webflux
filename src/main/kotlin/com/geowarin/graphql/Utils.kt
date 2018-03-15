package com.geowarin.graphql

import com.fasterxml.jackson.databind.type.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono
import java.util.*

val MapTypeRef: MapType =
  TypeFactory.defaultInstance().constructMapType(HashMap::class.java, String::class.java, Any::class.java)

fun ServerRequest.contentTypeIs(mediaType: MediaType) =
  this.headers().contentType().filter { it.isCompatibleWith(mediaType) }.isPresent

fun <T> ServerRequest.withBody(mapFun: (String) -> T): Mono<T> =
  this.bodyToMono<String>().flatMap { Mono.just(mapFun(it)) }

inline fun <reified T> readJson(value: String): T = jacksonObjectMapper().readValue(value, T::class.java)

fun readJsonMap(variables: String?): Map<String, Any>? = jacksonObjectMapper().readValue(variables, MapTypeRef)

fun serveStatic(resource: Resource): (ServerRequest) -> Mono<ServerResponse> =
  { ServerResponse.ok().body(BodyInserters.fromResource(resource)) }
