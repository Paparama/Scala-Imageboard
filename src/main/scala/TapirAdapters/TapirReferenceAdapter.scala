package TapirAdapters

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, ReferenceCreateBody, ReferenceResponseHttp}
import ru.dins.scalashool.imageboard.models.ModelConverter

trait TapirReferenceAdapter[F[_]] {
  def getRef(id: Long): F[Either[ApiError, ReferenceResponseHttp]]
  def addRef(body: ReferenceCreateBody): F[Either[ApiError, ReferenceResponseHttp]]
  def deleteRef(id: Long):  F[Either[ApiError, Unit]]
}

object TapirReferenceAdapter {
  def apply[F[_] : Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F]) = new TapirReferenceAdapter[F] {
    override def getRef(id: Long): F[Either[ApiError, ReferenceResponseHttp]] = storage.getReference(id).flatMap{
      case Left(error) => Applicative[F].pure(Left(error))
      case Right(refDB) => Applicative[F].pure(Right(modelConverter.convertReference(refDB)))
    }

    override def addRef(body: ReferenceCreateBody): F[Either[ApiError, ReferenceResponseHttp]] = storage.createReference(body.text, body.postId, body.referenceTo).map(modelConverter.convertReference(_).asRight)

    override def deleteRef(id: Long): F[Either[ApiError, Unit]] = storage.deleteReference(id).map(_.asRight)
  }
}
