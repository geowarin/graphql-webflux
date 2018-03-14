package com.example.demo

import graphql.Scalars.*
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLList
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema

data class Person(
  val name: String,
  val age: Int
)

val persons = listOf(
  Person("Ada", 20),
  Person("Haskell", 42)
)

fun buildSchema(): GraphQLSchema {

  val person = GraphQLObjectType.newObject()
    .name("person")
    .field { f -> f.type(GraphQLString).name("name") }
    .field { f -> f.type(GraphQLInt).name("age") }
    .build()

  val personsQuery = GraphQLFieldDefinition.newFieldDefinition()
    .name("persons")
    .type(GraphQLList.list(person))
    .dataFetcher { _ -> persons }

  return GraphQLSchema.newSchema()
    .query(
      GraphQLObjectType.newObject()
        .name("query")
        .field(personsQuery)
    ).build()
}
