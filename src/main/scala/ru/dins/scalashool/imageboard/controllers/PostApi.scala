package ru.dins.scalashool.imageboard.controllers

import ru.dins.scalashool.imageboard.models.ResponseModels.{ApiError, PostCreationBody, SuccessCreation}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{endpoint, oneOf, statusMapping}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

object PostApi {

  val addPost =
    endpoint.post
      .description("create post")
      .in("api" / "post")
      .in(multipartBody[PostCreationBody])
      .out(jsonBody[SuccessCreation])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
        )
      )
}
