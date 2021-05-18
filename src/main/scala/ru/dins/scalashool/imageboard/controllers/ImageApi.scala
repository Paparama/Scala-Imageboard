package ru.dins.scalashool.imageboard.controllers

import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, SuccessCreation, ImageHttp, ImageUpload}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{endpoint, oneOf, path, statusMapping}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

object ImageApi {
  val getImage =
    endpoint.get
      .description("get by Id")
      .in("api" / "image"/ path[Long])
      .out(jsonBody[ImageHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val getImageAsFile =
    endpoint.get
      .description("get as file")
      .in("api" / "image"/ path[String])
      .out(jsonBody[ImageHttp])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )

  val addImage =
    endpoint.post
      .description("create image")
      .in("api" / "image")
      .in(multipartBody[ImageUpload])
      .out(jsonBody[SuccessCreation])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
        )
      )

  val deleteImage =
    endpoint.delete
      .description("delete image")
      .in("api" / "image" / path[Long])
      .errorOut(
        oneOf[ApiError](
          statusMapping(StatusCode.InternalServerError, jsonBody[ApiError]),
          statusMapping(StatusCode.NotFound, jsonBody[ApiError]),
        )
      )
}
