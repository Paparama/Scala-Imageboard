package ru.dins.scalashool.imageboard

import ru.dins.scalashool.imageboard.TapirAdapters.{TapirBoardAdapter, TapirImageAdapter, TapirPostAdapter, TapirReferenceAdapter, TapirTopicAdapter}
import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.semigroupk._
import com.typesafe.config.ConfigFactory
import ru.dins.scalashool.imageboard.controllers.{BoardApi, ImageApi, PostApi, ReferenceApi, TopicApi}
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
  override def run(args: List[String]): IO[ExitCode] =  IO.delay(ConfigFactory.load()).flatMap{ config =>

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

    val blocker = Blocker.liftExecutionContext(executionContext)

    val storage           = PostgresStorage(xa)
    val modelConverter    = ModelConverter(storage)
    val tapirPostService  = TapirPostAdapter(storage, modelConverter)
    val tapirTreadService = TapirTopicAdapter(storage, modelConverter)
    val tapirRefAdapter   = TapirReferenceAdapter(storage, modelConverter)
    val tapirBoardAdapter = TapirBoardAdapter(storage, modelConverter)
    val tapirImageAdapter = TapirImageAdapter(storage, modelConverter)

    val getPostRoute    = Http4sServerInterpreter.toRoutes(PostApi.getPost)(tapirPostService.getPost)
    val addPostRoute    = Http4sServerInterpreter.toRoutes(PostApi.addPost)(tapirPostService.addPost)
    val updatePostRoute = Http4sServerInterpreter.toRoutes(PostApi.updatePost)(tapirPostService.updatePost)
    val deletePostRoute = Http4sServerInterpreter.toRoutes(PostApi.deletePost)(tapirPostService.deletePost)

    val getTreadRoute    = Http4sServerInterpreter.toRoutes(TopicApi.getTopic)(tapirTreadService.getTread)
    val addTreadRoute    = Http4sServerInterpreter.toRoutes(TopicApi.addTopic)(tapirTreadService.addTread)
    val updateTreadRoute = Http4sServerInterpreter.toRoutes(TopicApi.updateTopic)(tapirTreadService.updateTread)
    val deleteTreadRoute = Http4sServerInterpreter.toRoutes(TopicApi.deleteTopic)(tapirTreadService.deleteTread)

    val getBoardRoute    = Http4sServerInterpreter.toRoutes(BoardApi.getBoard)(tapirBoardAdapter.getBoard)
    val addBoardRoute    = Http4sServerInterpreter.toRoutes(BoardApi.addBoard)(tapirBoardAdapter.addBoard)
    val deleteBoardRoute = Http4sServerInterpreter.toRoutes(BoardApi.deleteBoard)(tapirBoardAdapter.deleteBoard)

    val getRefRoute    = Http4sServerInterpreter.toRoutes(ReferenceApi.getReference)(tapirRefAdapter.getRef)
    val addRefRoute    = Http4sServerInterpreter.toRoutes(ReferenceApi.addRef)(tapirRefAdapter.addRef)
    val deleteRefRoute = Http4sServerInterpreter.toRoutes(ReferenceApi.deleteRef)(tapirRefAdapter.deleteRef)

    val getImageRoute    = Http4sServerInterpreter.toRoutes(ImageApi.getImage)(tapirImageAdapter.getImage)
    val addImageRoute    = Http4sServerInterpreter.toRoutes(ImageApi.addImage) (tapirImageAdapter.addImage(_)(blocker))
    val deleteImageRoute = Http4sServerInterpreter.toRoutes(ImageApi.deleteImage)(tapirImageAdapter.deleteImage)

    val treadRoutsList = List(TopicApi.getTopic, TopicApi.addTopic, TopicApi.updateTopic, TopicApi.deleteTopic)
    val postRoutsList  = List(PostApi.getPost, PostApi.addPost, PostApi.updatePost, PostApi.deletePost)
    val boardRoutsList = List(BoardApi.getBoard, BoardApi.addBoard, BoardApi.deleteBoard)
    val refRoutsList   = List(ReferenceApi.getReference, ReferenceApi.addRef, ReferenceApi.deleteRef)

    val docs = OpenAPIDocsInterpreter.toOpenAPI(treadRoutsList ++ postRoutsList ++ boardRoutsList ++ refRoutsList, "Image Board API", "1.0")

    val swagger = new SwaggerHttp4s(docs.toYaml)

    val httpApp = Router(
      "/" -> (getPostRoute <+> addPostRoute <+> updatePostRoute <+> deletePostRoute <+> getTreadRoute <+>
        addTreadRoute <+> updateTreadRoute <+> deleteTreadRoute <+> getBoardRoute <+> addBoardRoute <+>
        deleteBoardRoute <+> getRefRoute <+> addRefRoute <+> deleteRefRoute <+> getImageRoute <+> addImageRoute <+> deleteImageRoute),
      "/swagger" -> swagger.routes, // http://localhost:8080/swagger/docs
    ).orNotFound

    val server: Resource[IO, Server[IO]] = BlazeServerBuilder[IO](ExecutionContext.global)
      .bindLocal(8080)
      .withHttpApp(httpApp)
      .resource

    Migrations.migrate(xa) >> server.use(_ => IO.never).as(ExitCode.Success)
  }
}
