package ru.dins.scalashool.imageboard.controllers

import cats.effect.{Blocker, ContextShift, Sync}
import org.http4s._
import org.http4s.dsl.Http4sDsl

import java.io.File

object StaticFileRouter {

  def staticRoutes[F[_]: Sync :ContextShift](blocker: Blocker): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] {
      case request@GET -> Root /"uploadedFiles"/topicId/uuid/fileName if List(".png", ".jpeg").exists(fileName.endsWith)  =>
        StaticFile
          .fromFile(new File(s"uploadedFiles/$topicId/$uuid/$fileName"), blocker, Some(request))
          .getOrElseF(NotFound())
    }
  }
}
