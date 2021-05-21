package ru.dins.scalashool.imageboard.controllers

import io.circe.generic.auto._
import ru.dins.scalashool.imageboard.models.{ApiError, NotFound, UnprocessableEntity}
import ru.dins.scalashool.imageboard.models.ResponseModels.{SuccessCreation, TopicCreationBody, TopicResponse}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object TopicApi {

  val getTopic =
    endpoint.get
      .description("get topic by Id")
      .in("api" / "topic"/ path[Long])
      .out(jsonBody[TopicResponse])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.NotFound, jsonBody[NotFound]),
        )
      )

  val addTopic =
    endpoint.post
      .description("create topic")
      .in("api" / "topic")
      .in(jsonBody[TopicCreationBody])
      .out(jsonBody[SuccessCreation])
      .out(statusCode(StatusCode.Created))
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.UnprocessableEntity, jsonBody[UnprocessableEntity]),
        )
      )
}
