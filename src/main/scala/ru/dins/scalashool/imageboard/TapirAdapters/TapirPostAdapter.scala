package ru.dins.scalashool.imageboard.TapirAdapters

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.ResponseModels.{ApiError, PostCreationBody,  SuccessCreation}
import ru.dins.scalashool.imageboard.models.ModelConverter

trait TapirPostAdapter[F[_]] {

  def addPost(body: PostCreationBody): F[Either[ApiError, SuccessCreation]]
  def deletePost(id: Long):  F[Either[ApiError, Unit]]

}

object TapirPostAdapter {
  def apply[F[_]: Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F]) = new TapirPostAdapter[F] {

    override def addPost(body: PostCreationBody): F[Either[ApiError, SuccessCreation]] = body match {
      case PostCreationBody(treadId, text, references, imageIds) => storage.createPost(treadId, text, references, imageIds).flatMap{post => Applicative[F].pure(Right(SuccessCreation(s"Post with id ${post.id} was created")))}
    }

    override def deletePost(id: Long): F[Either[ApiError, Unit]] = storage.deletePost(id).map(_.asRight)
  }
}
