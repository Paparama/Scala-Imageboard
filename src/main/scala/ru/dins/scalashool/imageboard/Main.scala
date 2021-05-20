package ru.dins.scalashool.imageboard

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import cats.implicits.catsSyntaxFlatMapOps
import ru.dins.scalashool.imageboard.config.DbConfigLoader
import ru.dins.scalashool.imageboard.controllers.Routs
import doobie.Transactor
import org.flywaydb.core.Flyway
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.Server

import scala.concurrent.ExecutionContext


object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =  DbConfigLoader.load[IO].flatMap{ config =>

    val DB_DRIVER      = config.driver
    val DB_URL         = config.url
    val DB_USER        = config.user
    val DB_PASS        = config.password

    val xa = Transactor.fromDriverManager[IO](
      DB_DRIVER,
      DB_URL,
      DB_USER,
      DB_PASS,
    )

    val blocker = Blocker.liftExecutionContext(executionContext)

    val httpApp = Routs.getRouter(blocker, xa)

    val server: Resource[IO, Server[IO]] = BlazeServerBuilder[IO](ExecutionContext.global)
      .bindLocal(8080)
      .withHttpApp(httpApp)
      .resource

    IO.pure(new Flyway(Flyway.configure().dataSource(config.url, config.user, config.password)).migrate).void >> server.use(_ => IO.never).as(ExitCode.Success)
  }
}
