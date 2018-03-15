package com.geowarin.graphql

import graphql.Scalars.*
import graphql.schema.*

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
    .argument { a -> a.type(GraphQLString).name("nameLike") }
    .dataFetcher { e ->
      val nameArgument = e.getArgument<String?>("nameLike")
      if (nameArgument != null) {
        persons.filter { it.name.toLowerCase().contains(nameArgument.toLowerCase()) }
      } else {
        persons
      }
    }

  return GraphQLSchema.newSchema()
    .query(
      GraphQLObjectType.newObject()
        .name("query")
        .field(personsQuery)
    ).build()
}
