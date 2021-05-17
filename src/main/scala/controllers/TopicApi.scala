package controllers

import io.circe.generic.auto._
import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, TopicCreationBody, TopicHttp, TopicUpdateBody}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object TopicApi {

  val getTread =
    endpoint.get
      .description("get tread by Id")
      .in("api" / "tread"/ path[Long])
      .out(jsonBody[TopicHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val addTread =
    endpoint.post
      .description("create tread")
      .in("api" / "tread")
      .in(jsonBody[TopicCreationBody])
      .out(jsonBody[TopicHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.UnprocessableEntity, jsonBody[ApiError]),
        )
      )

  val updateTread =
    endpoint.patch
      .description("update tread")
      .in("api" / "tread"/ path[Long])
      .in(jsonBody[TopicUpdateBody])
      .out(jsonBody[TopicHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val deleteTread =
    endpoint.delete
      .description("delete tread")
      .in("api" / "tread" / path[Long])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )
}
