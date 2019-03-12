package controllers

import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.util.Try

/**
  * Handles and parse GraphQL queries.
  *
  * @param controllerComponents base controller component dependencies that most controllers rely on
  */
class GraphQlHandler(controllerComponents: ControllerComponents) extends AbstractController(controllerComponents) {

  /**
    * Parses JSON into the GraphQL query components.
    *
    * @param json an instance of JsValue
    * @return a tuple that contains GraphQL query components, namely 'query' body, 'variables' and 'operationName'
    */
  def parseToGraphQLQuery(json: JsValue): Try[(String, Option[String], Option[JsObject])] = {
    val extract: JsValue => (String, Option[String], Option[JsObject]) = query =>
      (
        (query \ "query").as[String],
        (query \ "operationName").asOpt[String],
        (query \ "variables").toOption.flatMap {
          case JsString(vars) => Some(parseVariables(vars))
          case obj: JsObject => Some(obj)
          case _ => None
        }
      )
    Try {
      json match {
        case arrayBody@JsArray(_) => extract(arrayBody.value(0))
        case objectBody@JsObject(_) => extract(objectBody)
        case otherType =>
          throw new Error {
            s"The '/graphql' endpoint doesn't support request bodies of type [${otherType.getClass.getSimpleName}]"
          }
      }
    }
  }

  /**
    * Parses variables of the incoming GraphQL query.
    *
    * @param variables variables from the query
    * @return JsObject with variables
    */
  def parseVariables(variables: String): JsObject =
    if (variables.trim.isEmpty || variables.trim == "null") Json.obj()
    else Json.parse(variables).as[JsObject]

}
