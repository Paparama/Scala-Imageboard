package ru.dins.scalashool.imageboard

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import ru.dins.scalashool.imageboard.config.ConfigLoader
import ru.dins.scalashool.imageboard.controllers.Routs
import doobie.Transactor
import org.flywaydb.core.Flyway
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.Server
import ru.dins.scalashool.imageboard.mailClient.MailClient

import scala.concurrent.ExecutionContext



object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =  ConfigLoader.load[IO].flatMap{ config =>
    val xa = Transactor.fromDriverManager[IO](
      config.db.driver,
      config.db.url,
      config.db.user,
      config.db.password,
    )

    val mailClient = MailClient[IO](config.mail)

    val blocker = Blocker.liftExecutionContext(executionContext)

    val httpApp = Routs.getRouter(blocker, xa, mailClient, config.app)

    val server: Resource[IO, Server[IO]] = BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(config.app.port, "0.0.0.0")
      .withHttpApp(httpApp)
      .resource

    for {
      _ <- IO.delay {
        new Flyway(
          Flyway.configure().dataSource(
            config.db.url,
            config.db.user,
            config.db.password
          )
        ).migrate()
      }
      _ <- server.use(_ => IO.never)
    } yield ExitCode.Success
  }
}
