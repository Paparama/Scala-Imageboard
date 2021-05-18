package ru.dins.scalashool.imageboard.controllers

import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, BoardCreateBody, BoardHttp}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{endpoint, oneOf, path, statusMapping}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

object BoardApi {

  val getBoard =
    endpoint.get
      .description("get by Id")
      .in("api" / "board"/ path[Long])
      .out(jsonBody[BoardHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val addBoard =
    endpoint.post
      .description("create board")
      .in("api" / "board")
      .in(jsonBody[BoardCreateBody])
      .out(jsonBody[BoardHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
        )
      )

  val deleteBoard =
    endpoint.delete
      .description("delete board")
      .in("api" / "board" / path[Long])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )
}
