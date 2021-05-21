package ru.dins.scalashool.imageboard.TapirAdapters

import cats.effect.{Async, Blocker, Concurrent, ContextShift, Sync}
import cats.implicits._
import ru.dins.scalashool.imageboard.config.AppConfigModel
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.mailClient.MailClient
import ru.dins.scalashool.imageboard.models.ResponseModels.{PostCreationBody, SuccessCreation}
import ru.dins.scalashool.imageboard.models.{ApiError, ModelConverter, UnprocessableEntity, WrongContentType}

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Path}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.io.Directory

trait TapirPostAdapter[F[_]] {

  def addPost(body: PostCreationBody)(blocker: Blocker): F[Either[ApiError, SuccessCreation]]

}

object TapirPostAdapter {
  def apply[F[_]: ContextShift: Sync: Async: Concurrent](
      storage: PostgresStorage[F],
      modelConverter: ModelConverter[F],
      mailClient: MailClient[F],
      config: AppConfigModel
  )(
      blocker: Blocker,
  ) = new TapirPostAdapter[F] {

    private def getInputStream(file: File): F[FileInputStream] =
      Sync[F].delay(new FileInputStream(file))

    private def getOutputStream(file: File): F[FileOutputStream] =
      Sync[F].delay(new FileOutputStream(file))

    private def createDir(path: Path): F[Unit] =
      Sync[F].delay(Files.createDirectories(path)).void

    // todo tapir have issues with uploading list of files, it read first file media-type only, one header for all multipart request
    private def addImage(image: File, mediaType: String, pathToFile: String)(blocker: Blocker): F[String] =
      (for {
        inSt <- getInputStream(image)
        path = Path.of(s"$pathToFile/" + image.getName + s".${mediaType.replace("image/", "")}")
        _     <- createDir(Path.of(pathToFile))
        outSt <- getOutputStream(path.toFile)
        _     <- blocker.delay[F, Unit](inSt.transferTo(outSt))
      } yield (inSt, outSt, path.toString)).flatMap { res =>
        res._1.close()
        res._2.close()
        Sync[F].delay(res._3)
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

    private def referenceChecker(refs: List[(Long, String)]): F[Either[ApiError, List[(Long, String)]]] =
      storage.getCountPostsById(refs._1F).flatMap {
        case count if count == refs.length => Sync[F].delay(refs.asRight)
        case _                             => Sync[F].delay(UnprocessableEntity("Not existed post ids at references").asLeft)
      }

    private def getMediaHeader(body: PostCreationBody): String =
      body.images.header("Content-Type").getOrElse("not found")

    private def checkMediaHeader(body: PostCreationBody): Boolean =
      if (body.images.body.nonEmpty) {
        getMediaHeader(body) match {
          case "image/jpeg" => true
          case "image/png"  => true
          case _            => false
        }
      } else true

    private def deleteFromFileSystem(topicId: Long): F[Unit] =
      Sync[F].delay(new Directory(Path.of(s"${config.uploadDir}/$topicId").toFile).deleteRecursively()).void

    private def deleteTopic(topicId: Long): F[Unit] =
      for {
        _ <- storage.deleteTopic(topicId)
        _ <- deleteFromFileSystem(topicId)
      } yield ()

    private def senNotification(topicId: Long, postText: String): F[Unit] =
      for {
        emails <- storage.getSubscribers(topicId)
        _      <- mailClient.send(emails, postText)
      } yield ()

    private def generateNames(number: Int, topicId: Long): List[String] = number match {
      case negative if negative <= 0 => List()
      case _                         => (0 to number - 1).collect(_ => s"${config.uploadDir}/$topicId/${UUID.randomUUID().toString}").toList
    }

    override def addPost(body: PostCreationBody)(blocker: Blocker): F[Either[ApiError, SuccessCreation]] = {
      val listOfRefs = referenceParser(body.text)
      referenceChecker(listOfRefs).flatMap {
        case Left(er) => Sync[F].delay(Left(er))
        case _ =>
          body.images.body match {
            case lst =>
              if (checkMediaHeader(body)) {
                Sync[F].delay(generateNames(lst.length, body.topicId)).flatMap { listOfPath =>
                  storage
                    .createPostTransaction(body.topicId, body.text, listOfRefs, listOfPath)
                    .flatMap {
                      case Left(er) => Sync[F].delay(Left(er))
                      case Right(postData) =>
                        lst
                          .zip(listOfPath)
                          .map(fileAndDir =>
                            addImage(fileAndDir._1, getMediaHeader(body), fileAndDir._2)(blocker),
                          )
                          .sequence
                          .flatMap { _ =>
                            (for {
                              _      <- Async.shift(global) *> Concurrent[F].start(senNotification(body.topicId, body.text))
                              pCount <- storage.getPostCount(body.topicId)
                            } yield pCount).flatMap { pCount =>
                              if (pCount < config.topicLimit) Sync[F].delay(SuccessCreation(s"Post with id ${postData.id} created").asRight)
                              else
                                deleteTopic(body.topicId) >> Sync[F]
                                  .delay(SuccessCreation(s"Post with id ${postData.id} created, but limit for topic was exceeded").asRight)
                            }
                          }
                    }
                }
              } else Sync[F].delay(WrongContentType("Unsupported Media Type").asLeft)
          }
      }
    }
  }
}
