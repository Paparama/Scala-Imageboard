package controllers

import io.circe.generic.auto._
import ru.dins.scalashool.imageboard.models.Models.{ApiError, Post, PostCreationBody}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object PostApi {

  val getPost =
    endpoint.get
      .description("get by Id")
      .in("api" / "posts"/ path[Long])
      .out(jsonBody[Post])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val addPost =
    endpoint.post
      .description("create post")
      .in("api" / "posts")
      .in(jsonBody[PostCreationBody])
      .out(jsonBody[Post])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )
}
