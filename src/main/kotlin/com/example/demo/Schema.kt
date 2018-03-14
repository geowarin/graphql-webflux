package com.example.demo

import graphql.Scalars
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema

fun buildSchema(): GraphQLSchema {
  return GraphQLSchema.newSchema()
    .query(
      GraphQLObjectType.newObject()
        .name("query")
        .field { field ->
          field
            .name("test")
            .type(Scalars.GraphQLString)
            .dataFetcher { environment -> "response" }
        }
        .build())
    .build()
}
