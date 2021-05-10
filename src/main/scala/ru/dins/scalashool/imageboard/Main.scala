package ru.dins.scalashool.imageboard

import cats.effect.Resource
import cats.implicits.catsSyntaxFlatMapOps
import doobie.Transactor
import org.http4s.server.{Router, Server}
import com.typesafe.config.{Config, ConfigFactory}
import controllers.{PostApi, TapirAdapter}
import ru.dins.scalashool.imageboard.db.{Migrations, PostgresStorage}
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import cats.syntax.semigroupk._


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

    val storage = PostgresStorage(xa)
    val tapirService   = TapirAdapter(storage)
    val getPostRoute    = Http4sServerInterpreter.toRoutes(PostApi.getPost)(tapirService.getPost)
    val addPostRoute    = Http4sServerInterpreter.toRoutes(PostApi.addPost)(tapirService.addPost)
    val docs           = OpenAPIDocsInterpreter.toOpenAPI(List(PostApi.getPost, PostApi.addPost), "Post API", "1.0")

    val swagger = new SwaggerHttp4s(docs.toYaml)

    val httpApp = Router(
      "/"        -> (getPostRoute <+> addPostRoute),
      "/swagger" -> swagger.routes, // http://localhost:8080/swagger/docs
    ).orNotFound


    val server: Resource[IO, Server[IO]] = BlazeServerBuilder[IO](ExecutionContext.global)
      .bindLocal(8080)
      .withHttpApp(httpApp)
      .resource

    Migrations.migrate(xa) >> server.use(_ => IO.never).as(ExitCode.Success)
  }
}
