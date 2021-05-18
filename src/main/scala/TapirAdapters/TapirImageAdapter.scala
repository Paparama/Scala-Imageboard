package TapirAdapters

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, ImageHttp, ImageUpload}
import ru.dins.scalashool.imageboard.models.ModelConverter

import java.io.{File, FileInputStream, FileOutputStream}

trait TapirImageAdapter[F[_]] {
  def getImage(id: Long): F[Either[ApiError, ImageHttp]]
  def addImage(body: ImageUpload): F[Either[ApiError, ImageHttp]]
  def deleteImage(id: Long):  F[Either[ApiError, Unit]]
}

object TapirImageAdapter {

  def apply[F[_] : Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F]) = new TapirImageAdapter[F] {
    override def getImage(id: Long): F[Either[ApiError, ImageHttp]] =  storage.getImage(id).flatMap{
      case Left(error) => Applicative[F].pure(Left(error))
      case Right(imageDB) => Applicative[F].pure(Right(modelConverter.convertImage(imageDB)))
    }
    override def addImage(body: ImageUpload): F[Either[ApiError, ImageHttp]] = {new FileOutputStream(body.data.body) getChannel() transferFrom(
      new FileInputStream(new File(s"/${body.postId}/" + body.data.name)) getChannel(), 0, Long.MaxValue )
      Applicative[F].pure(ImageHttp(s"/${body.postId}/" + body.data.name).asRight)
    }


    override def deleteImage(id: Long): F[Either[ApiError, Unit]] = storage.deleteImage(id).map(_.asRight)
  }
}