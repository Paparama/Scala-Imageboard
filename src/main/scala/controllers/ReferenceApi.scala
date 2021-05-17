package controllers

import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, ReferenceCreateBody, ReferenceResponseHttp}
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
      .out(jsonBody[ReferenceResponseHttp])
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
      .out(jsonBody[ReferenceResponseHttp])
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
