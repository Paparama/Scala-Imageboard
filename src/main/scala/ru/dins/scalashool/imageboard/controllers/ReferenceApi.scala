package ru.dins.scalashool.imageboard.controllers

import ru.dins.scalashool.imageboard.models.ResponseModels.{ApiError, ReferenceCreateBody, ReferenceResponse, SuccessCreation}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{endpoint, oneOf, path, statusMapping}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

object ReferenceApi {

  val getReference =
    endpoint.get
      .description("get by Id")
      .in("api" / "reference"/ path[Long])
      .out(jsonBody[ReferenceResponse])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val addRef =
    endpoint.post
      .description("create reference")
      .in("api" / "reference")
      .in(jsonBody[ReferenceCreateBody])
      .out(jsonBody[SuccessCreation])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
        )
      )

  val deleteRef =
    endpoint.delete
      .description("delete reference")
      .in("api" / "reference" / path[Long])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )
}
