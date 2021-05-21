package ru.dins.scalashool.imageboard.controllers

import ru.dins.scalashool.imageboard.models.ResponseModels.{ApiError, BoardCreateBody, BoardResponse, ListOfBoardsResponse, SuccessCreation}
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
      .out(jsonBody[BoardResponse])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val getBoards =
    endpoint.get
      .description("get all boards")
      .in("api" / "board")
      .out(jsonBody[ListOfBoardsResponse])
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
      .out(jsonBody[SuccessCreation])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
        )
      )
}
