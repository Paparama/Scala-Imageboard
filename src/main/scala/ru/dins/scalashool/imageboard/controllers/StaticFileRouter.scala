package ru.dins.scalashool.imageboard.controllers

import cats.effect.{Blocker, ContextShift, Sync}
import org.http4s.dsl.Http4sDsl
import org.http4s.{StaticFile, _}
import ru.dins.scalashool.imageboard.config.AppConfigModel

import java.io.File

object StaticFileRouter {

  def staticRoutes[F[_]: Sync: ContextShift](config: AppConfigModel)(blocker: Blocker): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case request @ GET -> Root / topicId / uuid =>
      val files = new File(s"${config.uploadDir}/$topicId/$uuid/").listFiles()
      if (files != null) StaticFile.fromFile(files.head, blocker, Some(request)).getOrElseF(NotFound())
      else NotFound("there is no such files!")
    }
  }
}
