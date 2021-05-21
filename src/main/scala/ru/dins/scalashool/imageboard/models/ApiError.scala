package ru.dins.scalashool.imageboard.models

sealed trait ApiError
case class NotFound(errorMessage: String)  extends ApiError
case class BadRequest(errorMessage: String)  extends ApiError
case class WrongContentType(errorMessage: String)  extends ApiError
case class ServerError(errorMessage: String)  extends ApiError
case class UnprocessableEntity(errorMessage: String)  extends ApiError
