package com.example.demo

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.test.test
import org.assertj.core.api.Assertions.*

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class DemoApplicationTests(@LocalServerPort port: Int) {
  private val client = WebClient.create("http://localhost:$port")

  @Test
  fun contextLoads() {
    client.post().uri("/graphql")
      .syncBody(QueryParameters(
        query = "{test}"
      ))
      .retrieve().bodyToMono<String>()
      .test().consumeNextWith {
        assertThat(it).isEqualTo("""{"data":{"test":"response"},"errors":[],"extensions":null}""")
      }
      .verifyComplete()
  }

}

data class GraphqlResponse(
  val data: String,
  val errors: String,
  val extensions: String?
)
