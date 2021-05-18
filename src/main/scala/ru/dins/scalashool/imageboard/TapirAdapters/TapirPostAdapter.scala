package ru.dins.scalashool.imageboard.TapirAdapters

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, PostCreationBody, PostHttp, PostUpdateBody}
import ru.dins.scalashool.imageboard.models.ModelConverter

trait TapirPostAdapter[F[_]] {

  def getPost(id: Long): F[Either[ApiError, PostHttp]]
  def addPost(body: PostCreationBody): F[Either[ApiError, PostHttp]]
  def updatePost(idAndBody: (Long, PostUpdateBody)): F[Either[ApiError, PostHttp]]
  def deletePost(id: Long):  F[Either[ApiError, Unit]]

}

object TapirPostAdapter {
  def apply[F[_]: Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F]) = new TapirPostAdapter[F] {
    override def getPost(id: Long): F[Either[ApiError, PostHttp]] =
      storage.getPost(id).flatMap{
        case Left(error) => Applicative[F].pure(Left(error))
        case Right(postDB) => modelConverter.convertPost(postDB)
      }

    override def addPost(body: PostCreationBody): F[Either[ApiError, PostHttp]] = body match {
      case PostCreationBody(treadId, text, references, imageIds) => storage.createPost(treadId, text, references, imageIds).flatMap{modelConverter.convertPost}
    }

    override def updatePost(idAndBody: (Long, PostUpdateBody)): F[Either[ApiError, PostHttp]] = idAndBody._2 match {
      case PostUpdateBody(refRespIds, refFromIds, imageIds) => storage.updatePost(idAndBody._1, refRespIds, refFromIds, imageIds).flatMap{
        case Left(error) => Applicative[F].pure(Left(error))
        case Right(postDB) => modelConverter.convertPost(postDB)
      }
    }

    override def deletePost(id: Long): F[Either[ApiError, Unit]] = storage.deletePost(id).map(_.asRight)
  }
}
