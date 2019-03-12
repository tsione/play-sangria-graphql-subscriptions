package controllers

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import graphql.{GraphQL, GraphQLSubscriptions, UserContext}
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import sangria.ast.Document
import sangria.ast.OperationType.Subscription
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.playJson._
import sangria.parser.QueryParser
import sangria.streaming.akkaStreams.AkkaSource

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object WebSocketFlowActor {

  /**
    * Returns an instance of Props for the WebSocketFlowActor actor.
    *
    * @param outActor             an actor on which will be sent messages from the current actor.
    *                             Messages received by 'outActor' will be sent to the client over WebSockets
    * @param graphQL              an object containing a graphql schema of the entire application
    * @param controllerComponents base controller components dependencies that most controllers rely on
    * @param ec                   execute program logic asynchronously, typically but not necessarily on a thread pool
    * @param mat                  an instance of an implementation of Materializer Service Provider Interface
    * @return an instance of Props for the WebSocketFlowActor actor
    */
  def props(outActor: ActorRef, graphQL: GraphQL, controllerComponents: ControllerComponents)
           (implicit ec: ExecutionContext, mat: Materializer): Props = {
    Props(new WebSocketFlowActor(outActor, graphQL, GraphQLSubscriptions(), controllerComponents))
  }
}

/**
  * WebSocketFlowActor is the actor that receives messages sent by the client over WebSockets.
  *
  * @param outActor             an actor on which messages are sent from the current actor.
  *                             Messages received by 'outActor' will be sent to the client over WebSockets
  * @param graphQL              an object containing a GraphQL schema of the entire application
  * @param graphQLSubscriptions an instance that contains GraphQL subscriptions which can be canceled on demand
  * @param controllerComponents base controller components dependencies that most controllers rely on
  * @param ec                   execute program logic asynchronously, typically but not necessarily on a thread pool
  * @param mat                  an instance of an implementation of Materializer Service Provider Interface
  */
class WebSocketFlowActor(outActor: ActorRef,
                         graphQL: GraphQL,
                         graphQLSubscriptions: GraphQLSubscriptions,
                         controllerComponents: ControllerComponents)
                        (implicit ec: ExecutionContext,
                         mat: Materializer)
  extends GraphQlHandler(controllerComponents) with Actor {

  /** @inheritdoc */
  override def postStop(): Unit = {
    graphQLSubscriptions.cancelAll()
  }

  /** @inheritdoc */
  override def receive: Receive = {
    case message: String =>

      val maybeQuery = Try(Json.parse(message)) match {
        case Success(json) => parseToGraphQLQuery(json)
        case Failure(error) => throw new Error(s"Failed to parse the request body. Reason [$error]")
      }

      val source: AkkaSource[JsValue] = maybeQuery match {
        case Success((query, operationName, variables)) => executeQuery(query, graphQL, variables, operationName)
        case Failure(error) => Source.single(JsString(error.getMessage))
      }
      source.map(_.toString).runWith(Sink.actorRef[String](outActor, PoisonPill))
  }

  /**
    * Analyzes and executes an incoming GraphQL subscription query and returns a stream of elements.
    *
    * @param query     a GraphQL query body
    * @param graphQL   an object containing a GraphQL schema of the entire application
    * @param variables the incoming variables passed in the query
    * @param operation an operation name
    * @param mat       an instance of an implementation of Materializer Service Provider Interface
    * @return an instance of AkkaSource which represents a stream of elements
    */
  def executeQuery(query: String,
                   graphQL: GraphQL,
                   variables: Option[JsObject] = None,
                   operation: Option[String] = None)
                  (implicit mat: Materializer): AkkaSource[JsValue] = QueryParser.parse(query) match {

    case Success(queryAst: Document) =>
      queryAst.operationType(operation) match {
        case Some(Subscription) =>
          import sangria.execution.ExecutionScheme.Stream
          import sangria.streaming.akkaStreams._

          val source: AkkaSource[JsValue] = Executor.execute(
            schema = graphQL.Schema,
            queryAst = queryAst,
            variables = variables.getOrElse(Json.obj()),
            userContext = UserContext(Some(graphQLSubscriptions))
          ).recover {
            case error: QueryAnalysisError => Json.obj("BadRequest" -> error.resolveError)
            case error: ErrorWithResolver => Json.obj("InternalServerError" -> error.resolveError)
          }
          source

        case _ => Source.single {
          Json.obj("UnsupportedType" -> JsString(s"$operation"))
        }
      }

    case Failure(ex) => Source.single {
      Json.obj("BadRequest" -> JsString(s"${ex.getMessage}"))
    }
  }
}
