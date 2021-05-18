package ru.dins.scalashool.imageboard.controllers

import io.circe.generic.auto._
import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, TopicCreationBody, TopicHttp, TopicUpdateBody}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object TopicApi {

  val getTopic =
    endpoint.get
      .description("get topic by Id")
      .in("api" / "topic"/ path[Long])
      .out(jsonBody[TopicHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val addTopic =
    endpoint.post
      .description("create topic")
      .in("api" / "topic")
      .in(jsonBody[TopicCreationBody])
      .out(jsonBody[TopicHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.UnprocessableEntity, jsonBody[ApiError]),
        )
      )

  val updateTopic =
    endpoint.patch
      .description("update topic")
      .in("api" / "topic"/ path[Long])
      .in(jsonBody[TopicUpdateBody])
      .out(jsonBody[TopicHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val deleteTopic =
    endpoint.delete
      .description("delete topic")
      .in("api" / "topic" / path[Long])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )
}
