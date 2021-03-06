package ru.dins.scalashool.imageboard.controllers

import cats.Parallel
import cats.effect.{Blocker, Concurrent, ContextShift, Timer}
import org.http4s.server.Router
import cats.syntax.semigroupk._
import doobie.util.transactor.Transactor.Aux
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import ru.dins.scalashool.imageboard.TapirAdapters.{TapirBoardAdapter, TapirPostAdapter, TapirSubscribeAdapter, TapirTopicAdapter}
import ru.dins.scalashool.imageboard.config.AppConfigModel
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.mailClient.MailClient
import ru.dins.scalashool.imageboard.models.ModelConverter
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import sttp.tapir.openapi.circe.yaml._


object Routs {
  def getRouter[F[_]: Concurrent: ContextShift: Timer: Parallel](blocker: Blocker, xa: Aux[F, Unit], mailClient: MailClient[F], config: AppConfigModel) = {
    val storage           = PostgresStorage(xa)
    val modelConverter    = ModelConverter(storage)
    val tapirPostService  = TapirPostAdapter(storage, modelConverter, mailClient, config)(blocker)
    val tapirTreadService = TapirTopicAdapter(storage, modelConverter)
    val tapirBoardAdapter = TapirBoardAdapter(storage, modelConverter)
    val tapirSubscribeAdapter = TapirSubscribeAdapter(storage)

    val addPostRoute    = Http4sServerInterpreter.toRoutes(PostApi.addPost)(tapirPostService.addPost(_)(blocker))

    val getTreadRoute    = Http4sServerInterpreter.toRoutes(TopicApi.getTopic)(tapirTreadService.getTopic)
    val addTreadRoute    = Http4sServerInterpreter.toRoutes(TopicApi.addTopic)(tapirTreadService.addTopic)

    val getBoardRoute    = Http4sServerInterpreter.toRoutes(BoardApi.getBoard)(tapirBoardAdapter.getBoard)
    val getBoardsRoute    = Http4sServerInterpreter.toRoutes(BoardApi.getBoards)(_ => tapirBoardAdapter.getBoards)
    val addBoardRoute    = Http4sServerInterpreter.toRoutes(BoardApi.addBoard)(tapirBoardAdapter.addBoard)

    val addSubscription    = Http4sServerInterpreter.toRoutes(SubscribeApi.subscribe)(tapirSubscribeAdapter.addSubscribe)
    val deleteSubscription    = Http4sServerInterpreter.toRoutes(SubscribeApi.deleteSubscription)(tapirSubscribeAdapter.deleteSubscribe)

    val topicRoutsList = List(TopicApi.getTopic, TopicApi.addTopic)
    val postRoutsList  = List(PostApi.addPost)
    val boardRoutsList = List(BoardApi.getBoard, BoardApi.addBoard, BoardApi.getBoards)
    val subscriptionRoutsList = List(SubscribeApi.subscribe, SubscribeApi.deleteSubscription)

    val docs = OpenAPIDocsInterpreter.toOpenAPI(topicRoutsList ++ postRoutsList ++ boardRoutsList ++ subscriptionRoutsList, "Image Board API", "1.0")

    val swagger = new SwaggerHttp4s(docs.toYaml)

    Router(
      "/" -> (addPostRoute <+> getTreadRoute <+> getBoardsRoute <+>
        addTreadRoute  <+> getBoardRoute <+> addBoardRoute <+> addSubscription <+> deleteSubscription) ,
      "/swagger" -> swagger.routes, // http://localhost:8080/swagger/docs
      config.uploadDir -> StaticFileRouter.staticRoutes[F](config)(blocker),
    ).orNotFound
  }
}
