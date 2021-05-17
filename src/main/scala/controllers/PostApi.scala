package controllers

import io.circe.generic.auto._
import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, PostCreationBody, PostHttp, PostUpdateBody}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

object PostApi {

  val getPost =
    endpoint.get
      .description("get by Id")
      .in("api" / "posts"/ path[Long])
      .out(jsonBody[PostHttp])
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
      .out(jsonBody[PostHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
        )
      )

  val updatePost =
    endpoint.patch
      .description("update post")
      .in("api" / "posts"/ path[Long])
      .in(jsonBody[PostUpdateBody])
      .out(jsonBody[PostHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val deletePost =
    endpoint.delete
      .description("delete post")
      .in("api" / "posts" / path[Long])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )
}