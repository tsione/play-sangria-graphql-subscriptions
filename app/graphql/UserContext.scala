package graphql

/**
  * A class used in the GraphQL context.
  *
  * @param graphQlSubs an instance of the GraphQLSubscriptions class that contains a list
  *                    of subscriptions and that was opened during one WebSocket connection
  *                    by a user. It can be canceled on demand.
  */
case class UserContext(graphQlSubs: Option[GraphQLSubscriptions] = None)
