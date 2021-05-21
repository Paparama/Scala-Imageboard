package ru.dins.scalashool.imageboard.TapirAdapters

import cats.Applicative
import cats.effect.{Blocker, ContextShift, Sync}
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.ModelConverter
import ru.dins.scalashool.imageboard.models.ResponseModels.{ApiError, PostCreationBody, SuccessCreation}

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path}
import java.util.UUID
import scala.reflect.io.Directory

trait TapirPostAdapter[F[_]] {

  def addPost(body: PostCreationBody)(blocker: Blocker): F[Either[ApiError, SuccessCreation]]

}

object TapirPostAdapter {
  def apply[F[_]: ContextShift: Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F])(
      blocker: Blocker,
  ) = new TapirPostAdapter[F] {

    private def getInputStream(file: File): F[FileInputStream] =
      Applicative[F].pure(new FileInputStream(file))

    private def getOutputStream(file: File): F[FileOutputStream] =
      Applicative[F].pure(new FileOutputStream(file))

    private def createDir(path: Path): F[Unit] =
      Applicative[F].pure(Files.createDirectories(path)).void

    // todo tapir have issues with uploading list of files, it read first file media-type only, one header for all multipart request
    private def addImage(image: File, mediaType: String, topicId: Long)(blocker: Blocker): F[String] = (for {
      inSt <- getInputStream(image)
      uuid = UUID.randomUUID().toString
      path = Path.of(s"uploadedFiles/$topicId/$uuid/" + image.getName + s".${mediaType.replace("image/","")}")
      _     <- createDir(Path.of(s"uploadedFiles/$topicId/$uuid"))
      outSt <- getOutputStream(path.toFile)
      _     <- blocker.delay[F, Unit](inSt.transferTo(outSt))
    } yield (inSt, outSt, path.toString)).flatMap { res =>
      res._1.close()
      res._2.close()
      Applicative[F].pure(res._3)
    }

    private def referenceParser(text: String): List[(Long, String)] = "[>]{2}[0-9]* ".r
      .findAllIn(text)
      .toList
      .map(it =>
        it.replace(">>", "")
          .strip()
          .toLong,
      )
      .zip(text.split("[>]{2}[0-9]* ").tail)

    private def getMediaHeader(body: PostCreationBody): String = body.images.header("Content-Type").getOrElse("not found")

    private def checkMediaHeader(body: PostCreationBody): Boolean = {
      getMediaHeader(body) match {
        case  "image/jpeg" => true
        case  "image/png" => true
      }
    }

    private def deleteFromFileSystem(topicId: Long): F[Unit] = Applicative[F].pure(new Directory(Path.of(s"uploadedFiles/$topicId").toFile).deleteRecursively()).void

    private def deleteTopic(topicId: Long): F[Unit] = {
      for {
        _ <- storage.deleteTopic(topicId)
        _ <- deleteFromFileSystem(topicId)
      }  yield ()
      }

    override def addPost(body: PostCreationBody)(blocker: Blocker): F[Either[ApiError, SuccessCreation]] = {
      val listOfRefs = referenceParser(body.text)
      body.images.body match {
        case lst =>
          if (checkMediaHeader(body)) {
            lst.map(it => addImage(it, getMediaHeader(body), body.topicId)(blocker)).sequence.flatMap { listOfPath =>
              storage
                .createPostTransaction(body.topicId, body.text, listOfRefs, listOfPath)
                .flatMap(_ => storage.getPostCount(body.topicId).flatMap { pCount =>
                  if (pCount < 10)  Applicative[F].pure(SuccessCreation("Post created").asRight)
                  else deleteTopic(body.topicId) >> Applicative[F].pure(SuccessCreation("Post created, but limit for topic was exceeded").asRight)
                }
                )
            }
          } else Applicative[F].pure(ApiError(415, "Unsupported Media Type").asLeft)
      }
    }

  }
}
