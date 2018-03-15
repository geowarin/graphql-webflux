package com.geowarin.graphql

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GraphQLApplication

fun main(args: Array<String>) {
  runApplication<GraphQLApplication>(*args)
}
