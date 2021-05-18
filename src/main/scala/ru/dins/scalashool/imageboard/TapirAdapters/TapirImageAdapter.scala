package ru.dins.scalashool.imageboard.TapirAdapters

import cats.Applicative
import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, ImageHttp, ImageUpload, SuccessCreation}
import ru.dins.scalashool.imageboard.models.ModelConverter

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path}

trait TapirImageAdapter[F[_]] {
  def getImage(id: Long): F[Either[ApiError, ImageHttp]]
  def addImage(body: ImageUpload)(blocker: Blocker): F[Either[ApiError, SuccessCreation]]
  def deleteImage(id: Long):  F[Either[ApiError, Unit]]
}

object TapirImageAdapter {



  def apply[F[_]: ContextShift : Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F]) = new TapirImageAdapter[F] {

    private def getInputStream(file: File): F[FileInputStream] = {
      Applicative[F].pure(new FileInputStream(file))
    }

    private def getOutputStream(file: File): F[FileOutputStream] = {
      Applicative[F].pure(new FileOutputStream(file))
    }

    private def createDir(path: Path): F[Unit] = {
      Applicative[F].pure(Files.createDirectories(path)).void
    }

    override def getImage(id: Long): F[Either[ApiError, ImageHttp]] =  storage.getImage(id).flatMap{
      case Left(error) => Applicative[F].pure(Left(error))
      case Right(imageDB) => Applicative[F].pure(Right(modelConverter.convertImage(imageDB)))
    }
    override def addImage(body: ImageUpload)(blocker: Blocker): F[Either[ApiError, SuccessCreation]] = for {
      inSt <- getInputStream(body.data.body)
      path = Path.of(s"uploadedFiles/${body.postId}/" + body.data.fileName.getOrElse(body.postId.toString))
      _ <- createDir(Path.of(s"uploadedFiles/${body.postId}"))
      outSt <- getOutputStream(path.toFile)
      _ <- blocker.delay[F, Unit](inSt.transferTo(outSt))
      image <- storage.createImage(path.toString, body.postId)
      _ <- storage.updatePost(body.postId, None, None, Some(List(image.id)))
    } yield SuccessCreation(s"image created successfully").asRight

    override def deleteImage(id: Long): F[Either[ApiError, Unit]] = storage.deleteImage(id).map(_.asRight)
  }
}