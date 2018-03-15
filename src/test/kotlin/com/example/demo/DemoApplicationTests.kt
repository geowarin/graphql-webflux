package com.example.demo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.test.test
import java.net.URLEncoder

/**
 * Implements the spec at https://graphql.org/learn/serving-over-http/
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class DemoApplicationTests(@LocalServerPort port: Int) {
  private val client = WebClient.create("http://localhost:$port")

  @Test
  fun `should handle post`() {
    client.post().uri("/graphql")
      .syncBody(GraphQLParameters(query = "{persons{name}}"))
      .retrieve().bodyToMono<GraphqlResponse<Persons>>()
      .test().consumeNextWith {
        assertThat(it.data.persons.map { it.name }).containsExactly("Ada", "Haskell")
      }
      .verifyComplete()
  }

  @Test
  fun `should handle get`() {
    client.get().uri("/graphql?query={query}", "{persons{name}}")
      .retrieve().bodyToMono<GraphqlResponse<Persons>>()
      .test().consumeNextWith {
        assertThat(it.data.persons.map { it.name }).containsExactly("Ada", "Haskell")
      }
      .verifyComplete()
  }

  @Test
  fun `GET should error 400 when no query is present`() {
    client.get().uri("/graphql")
      .retrieve()
      .onStatus({it != BAD_REQUEST }) { fail { "Should return 400 error" } }
      .bodyToMono<Any>().test().verifyComplete()
  }

  @Test
  fun `POST should error 400 when no query is present`() {
    client.post().uri("/graphql")
      .retrieve().onStatus({it != BAD_REQUEST }) { fail { "Should return 400 error" } }
      .bodyToMono<Any>().test().verifyComplete()
  }

  @Test
  fun `POST should handle variables`() {
    val query = """
      query (${'$'}name: String) {
        persons(nameLike: ${'$'}name) {
          name
        }
      }
    """

    client.post().uri("/graphql")
      .syncBody(GraphQLParameters(query = query, variables = mapOf("name" to "ada")))
      .retrieve().bodyToMono<GraphqlResponse<Persons>>()
      .test().consumeNextWith {
        assertThat(it.data.persons.map { it.name }).containsExactly("Ada")
      }
      .verifyComplete()
  }

  @Test
  fun `GET should handle variables`() {
    val query = """query (${'$'}name: String) { persons(nameLike: ${'$'}name) { name } }"""
    val variables = """{"name": "ada"}"""

    client.get().uri("/graphql?query={query}&variables={variables}", query, variables)
      .retrieve().bodyToMono<GraphqlResponse<Persons>>()
      .test().consumeNextWith {
        assertThat(it.data.persons.map { it.name }).containsExactly("Ada")
      }
      .verifyComplete()
  }

  @Test
  fun `GET should handle variables (URL encoded)`() {
    val query = """query (${'$'}name: String) { persons(nameLike: ${'$'}name) { name } }"""
    val variables = URLEncoder.encode("""{"name": "ada"}""", "UTF-8")

    client.get().uri("/graphql?query={query}&variables={variables}", query, variables)
      .retrieve().bodyToMono<GraphqlResponse<Persons>>()
      .test().consumeNextWith {
        assertThat(it.data.persons.map { it.name }).containsExactly("Ada")
      }
      .verifyComplete()
  }

  @Test
  fun `POST should handle query parameter`() {
    val query = """{persons{name}}"""

    client.post().uri("/graphql?query={query}", query)
      .retrieve().bodyToMono<GraphqlResponse<Persons>>()
      .test().consumeNextWith {
        assertThat(it.data.persons.map { it.name }).containsExactly("Ada", "Haskell")
      }
      .verifyComplete()
  }

  @Test
  fun `POST should give priority to query parameter`() {
    val unusedQuery = """{persons {age}}"""
    val realQuery = """{persons {name}}"""

    client.post().uri("/graphql?query={query}", realQuery)
      .syncBody(GraphQLParameters(query = unusedQuery))
      .retrieve().bodyToMono<GraphqlResponse<Persons>>()
      .test().consumeNextWith {
        assertThat(it.data.persons.map { it.name }).containsExactly("Ada", "Haskell")
      }
      .verifyComplete()
  }

  @Test
  fun `POST should handle application-GraphQL header`() {
    val query = """{persons {name}}"""

    client.post().uri("/graphql").contentType(GraphQLMediaType)
      .syncBody(query)
      .retrieve().bodyToMono<GraphqlResponse<Persons>>()
      .test().consumeNextWith {
        assertThat(it.data.persons.map { it.name }).containsExactly("Ada", "Haskell")
      }
      .verifyComplete()
  }
}

data class Persons(
  val persons: List<Person>
)

data class GraphqlResponse<out T>(
  val data: T,
  val errors: List<Any>,
  val extensions: Any?
)
