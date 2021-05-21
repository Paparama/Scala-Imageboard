package ru.dins.scalashool.imageboard.controllers

import ru.dins.scalashool.imageboard.models.ResponseModels.{PostCreationBody, SuccessCreation}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{endpoint, oneOf, statusMapping}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import ru.dins.scalashool.imageboard.models.{ApiError, UnprocessableEntity}

object PostApi {

  val addPost =
    endpoint.post
      .description("create post")
      .in("api" / "post")
      .in(multipartBody[PostCreationBody])
      .out(jsonBody[SuccessCreation])
      .out(statusCode(StatusCode.Created))
      .errorOut(
        oneOf[ApiError](
          statusMapping(
            StatusCode.UnprocessableEntity, jsonBody[UnprocessableEntity]),
        )
      )
}
