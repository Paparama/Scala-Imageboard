package ru.dins.scalashool.imageboard.TapirAdapters

import cats.Applicative
import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.ResponseModels.{ApiError, PostCreationBody, SuccessCreation}
import ru.dins.scalashool.imageboard.models.ModelConverter

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path}
import java.util.UUID

trait TapirPostAdapter[F[_]] {

  def addPost(body: PostCreationBody)(blocker: Blocker): F[Either[ApiError, SuccessCreation]]
  def deletePost(id: Long):  F[Either[ApiError, Unit]]

}

object TapirPostAdapter {
  def apply[F[_] :ContextShift : Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F])(blocker: Blocker) = new TapirPostAdapter[F] {

    private def getInputStream(file: File): F[FileInputStream] = {
      Applicative[F].pure(new FileInputStream(file))
    }

    private def getOutputStream(file: File): F[FileOutputStream] = {
      Applicative[F].pure(new FileOutputStream(file))
    }

    private def createDir(path: Path): F[Unit] = {
      Applicative[F].pure(Files.createDirectories(path)).void
    }

    private def addImage(image: File)(blocker: Blocker): F[String] = (for {
      inSt <- getInputStream(image)
      uuid = UUID.randomUUID().toString
      path = Path.of(s"uploadedFiles/$uuid/" + image.getName)
      _ <- createDir(Path.of(s"uploadedFiles/$uuid"))
      outSt <- getOutputStream(path.toFile)
      _ <- blocker.delay[F, Unit](inSt.transferTo(outSt))
    } yield (inSt, outSt, path.toString)).flatMap { res =>
      res._1.close()
      res._2.close()
      Applicative[F].pure(res._3)
    }

    private def referenceParser(text: String): List[(Long, String)] = "[>]{2}[0-9]* ".r
      .findAllIn(text)
      .toList
      .map(it => it.replace(">>", "")
        .strip().toLong)
      .zip(text.split("[>]{2}[0-9]* ").tail)


    override def addPost(body: PostCreationBody)(blocker: Blocker): F[Either[ApiError, SuccessCreation]] = {
      val listOfRefs = referenceParser(body.text)
      body.images.body match {
      case Nil => storage.createPostTransaction(body.topicId, body.text, listOfRefs, List()).flatMap(_ => Applicative[F].pure(SuccessCreation("Post created").asRight))
      case lst => lst.map(it => addImage(it)(blocker)).sequence.flatMap {
        listOfPath =>  storage.createPostTransaction(body.topicId, body.text, listOfRefs, listOfPath).flatMap(_ => Applicative[F].pure(SuccessCreation("Post created").asRight))
        }
      }
    }


    override def deletePost(id: Long): F[Either[ApiError, Unit]] = storage.deletePost(id).map(_.asRight)
  }
}
