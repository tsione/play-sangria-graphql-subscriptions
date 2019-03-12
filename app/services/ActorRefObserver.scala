package services

import akka.actor.ActorRef
import akka.actor.ActorRef.noSender
import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.reactive.Observer
import play.api.Logger

import scala.concurrent.Future

/**
  * An implementation of [[Observer]].
  *
  * @param actorRef a reference to an actor to which the incoming elements are sent from the observable
  * @tparam T the type of incoming elements from the observable
  */
case class ActorRefObserver[T](actorRef: ActorRef) extends Observer[T] {

  private val log = Logger(classOf[ActorRefObserver[T]])

  /**
    * All elements passed into the onNext() method will be sent to an actor using actorRef.
    *
    * @param elem an incoming element from observable
    * @return an instance of [[Ack]]. Sends back to upstream an acknowledgment of processing
    */
  override def onNext(elem: T): Future[Ack] = {
    actorRef.tell(elem, noSender)
    Continue
  }

  /** @inheritdoc */
  override def onError(ex: Throwable): Unit = {
    log.info(s"Error has occurred. Reason: ${ex.getCause}")
  }

  /** @inheritdoc */
  override def onComplete(): Unit = {
    log.info(s"Event stream has closed.")
  }
}
