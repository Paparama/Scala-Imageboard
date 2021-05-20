package ru.dins.scalashool.imageboard.controllers

import cats.effect.{Blocker, Concurrent, ContextShift, Timer}
import org.http4s.server.Router
import cats.syntax.semigroupk._
import doobie.util.transactor.Transactor.Aux
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import ru.dins.scalashool.imageboard.TapirAdapters.{TapirBoardAdapter, TapirImageAdapter, TapirPostAdapter, TapirReferenceAdapter, TapirTopicAdapter}
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.ModelConverter
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import sttp.tapir.openapi.circe.yaml._


object Routs {
  def getRouter[F[_]: Concurrent: ContextShift: Timer](blocker: Blocker, xa: Aux[F, Unit]) = {
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

    Router(
      "/" -> (getPostRoute <+> addPostRoute <+> updatePostRoute <+> deletePostRoute <+> getTreadRoute <+>
        addTreadRoute <+> updateTreadRoute <+> deleteTreadRoute <+> getBoardRoute <+> addBoardRoute <+>
        deleteBoardRoute <+> getRefRoute <+> addRefRoute <+> deleteRefRoute <+> getImageRoute <+> addImageRoute <+> deleteImageRoute),
      "/swagger" -> swagger.routes, // http://localhost:8080/swagger/docs
    ).orNotFound
  }
}
