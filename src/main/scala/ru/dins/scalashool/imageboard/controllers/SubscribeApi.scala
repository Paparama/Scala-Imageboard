package ru.dins.scalashool.imageboard.controllers

import ru.dins.scalashool.imageboard.models.ResponseModels.{SubscribeCreateBody, SuccessCreation, SuccessUnsubscribe, UnsubscribeBody}
import ru.dins.scalashool.imageboard.models.{ApiError, NotFound, ServerError, UnprocessableEntity}
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

object SubscribeApi {

  val subscribe =
    endpoint.post
      .description("create subscription to topic")
      .in("api" / "subscribe")
      .in(jsonBody[SubscribeCreateBody])
      .out(jsonBody[SuccessCreation])
      .out(statusCode(StatusCode.Created))
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.UnprocessableEntity, jsonBody[UnprocessableEntity]),
        )
      )

  val deleteSubscription =
    endpoint.delete
      .description("delete subscription")
      .in("api" / "subscribe")
      .in(jsonBody[UnsubscribeBody])
      .out(jsonBody[SuccessUnsubscribe])
      .out(statusCode(StatusCode.Created))
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.NotFound, jsonBody[NotFound]),
          statusMapping(StatusCode.InternalServerError, jsonBody[ServerError]),
        )
      )
}
