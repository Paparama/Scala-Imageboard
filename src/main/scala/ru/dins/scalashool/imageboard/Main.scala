package ru.dins.scalashool.imageboard

import TapirAdapters.{TapirPostAdapter, TapirTopicAdapter}
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.semigroupk._
import com.typesafe.config.{Config, ConfigFactory}
import controllers.{PostApi, TopicApi}
import doobie.Transactor
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import ru.dins.scalashool.imageboard.db.{Migrations, PostgresStorage}
import ru.dins.scalashool.imageboard.models.ModelConverter
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {

    val config: Config = ConfigFactory.load()
    val DB_DRIVER      = config.getString("db.driver")
    val DB_URL         = config.getString("db.url")
    val DB_USER        = config.getString("db.user")
    val DB_PASS        = config.getString("db.password")

    val xa = Transactor.fromDriverManager[IO](
      DB_DRIVER,
      DB_URL,
      DB_USER,
      DB_PASS,
    )

    val storage           = PostgresStorage(xa)
    val modelConverter    = ModelConverter(storage)
    val tapirPostService  = TapirPostAdapter(storage, modelConverter)
    val tapirTreadService = TapirTopicAdapter(storage, modelConverter)

    val getPostRoute    = Http4sServerInterpreter.toRoutes(PostApi.getPost)(tapirPostService.getPost)
    val addPostRoute    = Http4sServerInterpreter.toRoutes(PostApi.addPost)(tapirPostService.addPost)
    val updatePostRoute = Http4sServerInterpreter.toRoutes(PostApi.updatePost)(tapirPostService.updatePost)
    val deletePostRoute = Http4sServerInterpreter.toRoutes(PostApi.deletePost)(tapirPostService.deletePost)

    val getTreadRoute    = Http4sServerInterpreter.toRoutes(TopicApi.getTread)(tapirTreadService.getTread)
    val addTreadRoute    = Http4sServerInterpreter.toRoutes(TopicApi.addTread)(tapirTreadService.addTread)
    val updateTreadRoute = Http4sServerInterpreter.toRoutes(TopicApi.updateTread)(tapirTreadService.updateTread)
    val deleteTreadRoute = Http4sServerInterpreter.toRoutes(TopicApi.deleteTread)(tapirTreadService.deleteTread)

    val treadRoutsList = List(TopicApi.getTread, TopicApi.addTread, TopicApi.updateTread, TopicApi.deleteTread)
    val postRoutsList  = List(PostApi.getPost, PostApi.addPost, PostApi.updatePost, PostApi.deletePost)

    val docs = OpenAPIDocsInterpreter.toOpenAPI(treadRoutsList ++ postRoutsList, "Image Board API", "1.0")

    val swagger = new SwaggerHttp4s(docs.toYaml)

    val httpApp = Router(
      "/" -> (getPostRoute <+> addPostRoute <+> updatePostRoute <+> deletePostRoute <+> getTreadRoute <+>
        addTreadRoute <+> updateTreadRoute <+> deleteTreadRoute),
      "/swagger" -> swagger.routes, // http://localhost:8080/swagger/docs
    ).orNotFound

    val server: Resource[IO, Server[IO]] = BlazeServerBuilder[IO](ExecutionContext.global)
      .bindLocal(8080)
      .withHttpApp(httpApp)
      .resource

    Migrations.migrate(xa) >> server.use(_ => IO.never).as(ExitCode.Success)
  }
}
