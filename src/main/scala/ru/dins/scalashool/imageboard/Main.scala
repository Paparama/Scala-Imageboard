package ru.dins.scalashool.imageboard

import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits.catsSyntaxFlatMapOps
import doobie.Transactor
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import com.typesafe.config.{Config, ConfigFactory}
import ru.dins.scalashool.imageboard.db.{Migrations, PostgresStorage}

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {

    val config: Config = ConfigFactory.load()
    val DB_DRIVER = config.getString("db.driver")
    val DB_URL = config.getString("db.url")
    val DB_USER = config.getString("db.user")
    val DB_PASS = config.getString("db.password")

    val xa = Transactor.fromDriverManager[IO](
      DB_DRIVER,
      DB_URL,
      DB_USER,
      DB_PASS,
    )

    val server: Resource[IO, Server[IO]] = BlazeServerBuilder[IO](ExecutionContext.global)
      .bindLocal(8080)
      .withHttpApp(Controller[IO](PostgresStorage[IO](xa)).routes.orNotFound)
      .resource

    Migrations.migrate(xa) >> server.use(_ => IO.never).as(ExitCode.Success)
  }
}
