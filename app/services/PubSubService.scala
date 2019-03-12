package services

import akka.NotUsed
import akka.stream.scaladsl.Source
import graphql.UserContext
import sangria.schema.Action

/**
  * A service to publish or subscribe to events.
  *
  * @tparam T the published entity
  */
trait PubSubService[T] {

  /**
    * Publishes an event.
    */
  def publish(event: T)

  /**
    * Subscribes to the event by specified params.
    */
  def subscribe(eventNames: Seq[String])(implicit userContext: UserContext): Source[Action[Nothing, T], NotUsed]
}
