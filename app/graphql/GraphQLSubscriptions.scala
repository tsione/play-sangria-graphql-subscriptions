package graphql

import monix.execution.Cancelable
import play.api.Logger

import scala.collection.mutable.ArrayBuffer

/**
  * A class that contains a list of subscriptions and that was opened
  * during a WebSocket connection by a user and which can be canceled on demand.
  */
case class GraphQLSubscriptions(){

  private val log = Logger(classOf[GraphQLSubscriptions])
  private[this] val subscriptions: ArrayBuffer[Cancelable] = ArrayBuffer.empty[Cancelable]
  private var closed = false

  /**
    * Adds a new Cancelable to subscriptions.
    */
  def add(cancelable: Cancelable): Unit = this.synchronized {
    if (!closed) {
      cancelable +: subscriptions
    } else {
      log.info("WebSocket connection was already closed!")
    }
  }

  /**
    * Cancels all subscriptions opened during a single WebSocket connection
    * and clears the subscriptions queue.
    */
  def cancelAll(): Unit = this.synchronized {
    subscriptions.foreach(_.cancel())
    subscriptions.clear()
    closed = true
  }

  /**
    * Returns the number of opened subscriptions during a single WebSocket connection.
    *
    * @return a number of opened subscriptions
    */
  def subscriptionsCount: Int = {
    this.subscriptions.size
  }
}
