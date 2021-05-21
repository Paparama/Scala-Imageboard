package ru.dins.scalashool.imageboard.controllers

import ru.dins.scalashool.imageboard.models.ResponseModels.{BoardCreateBody, BoardResponse, ListOfBoardsResponse, SuccessCreation}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{endpoint, oneOf, path, statusMapping}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import ru.dins.scalashool.imageboard.models.{ApiError, NotFound, UnprocessableEntity}

object BoardApi {

  val getBoard =
    endpoint.get
      .description("get by Id")
      .in("api" / "board"/ path[Long])
      .out(jsonBody[BoardResponse])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.NotFound, jsonBody[NotFound]),
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
        )
      )

  val addBoard =
    endpoint.post
      .description("create board")
      .in("api" / "board")
      .in(jsonBody[BoardCreateBody])
      .out(jsonBody[SuccessCreation])
      .out(statusCode(StatusCode.Created))
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.UnprocessableEntity, jsonBody[UnprocessableEntity]),
        )
      )
}
