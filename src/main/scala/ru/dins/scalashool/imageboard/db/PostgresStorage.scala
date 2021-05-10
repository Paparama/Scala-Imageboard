package ru.dins.scalashool.imageboard.db

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres.sqlstate
import doobie.util.Read
import doobie.util.fragments.{set, setOpt, whereAnd, whereAndOpt}
import doobie.util.invariant.InvariantViolation
import doobie.util.transactor.Transactor.Aux
import ru.dins.scalashool.imageboard.Storage
import ru.dins.scalashool.imageboard.models.Models
import ru.dins.scalashool.imageboard.models.Models._

case class PostgresStorage[F[_]: Sync](xa: Aux[F, Unit]) extends Storage[F] {

  def idFilterFr(id: Long) = whereAnd(fr"""id = $id""")

  def getSomething[A: Read](id: Long, collection: CollectionsNameEnum.Value): F[Either[Models.ApiError, A]] = {
    val collectionsName = collection match {
      case CollectionsNameEnum.POSTS => fr"posts"
      case CollectionsNameEnum.TREADS => fr"treads"
      case CollectionsNameEnum.BOARDS => fr"boards"
      case CollectionsNameEnum.IMAGES => fr"images"
      case CollectionsNameEnum.REFERENCES => fr"post_references"
    }
    (fr"SELECT * FROM" ++ collectionsName ++ idFilterFr(id))
      .query[A]
      .option
      .transact(xa)
      .flatMap {
        case None => Applicative[F].pure(Left(ApiError(404, s"There is no $collection with id=$id")))
        case Some(value) => Applicative[F].pure(Right(value))
      }
  }

  def deleteSomething(id: Long, collection: CollectionsNameEnum.Value): F[Unit] =
    (fr"DELETE FROM ${collection.toString}" ++ idFilterFr(id)).update.run
      .transact(xa)
      .void

  def getListOfSomething[A: Read](
                             collection: CollectionsNameEnum.Value,
                             filter: Option[CollectionsNameEnum.Value],
                             filterId: Option[Long],
                           ): F[List[A]] = {
    val filterFr = whereAndOpt(filter.map { collectionFt =>
      fr"${collectionFt.toString} = $filterId"
    })
    (fr"SELECT * FROM ${collection.toString}" ++ filterFr)
      .query[A]
      .to[List]
      .transact(xa)
  }

  override def getPost(id: Long): F[Either[Models.ApiError, Models.Post]] = getSomething(id, CollectionsNameEnum.POSTS)

  override def getPosts(treadId: Option[Long]): F[List[Models.Post]] = treadId match {
    case None => getListOfSomething(CollectionsNameEnum.POSTS, None, None)
    case Some(id) => getListOfSomething(CollectionsNameEnum.POSTS, Some(CollectionsNameEnum.TREADS), Some(id))
  }

  override def deletePost(id: Long): F[Unit] = deleteSomething(id, CollectionsNameEnum.POSTS)

  override def createPost(
                           treadId: Long,
                           text: String,
                           references: Option[List[Long]],
                           imageIds: Option[List[Long]],
                         ): F[Post] =
    sql"""INSERT INTO posts (image_ids, text, created_at, references_responses, tread_id)
          values ($imageIds, $text, current_timestamp, $references, $treadId)""".update
      .withUniqueGeneratedKeys[Post](
        "id",
        "image_ids",
        "text",
        "created_at",
        "references_responses",
        "references_from",
        "tread_id",
      )
      .transact(xa)

  override def getTread(id: Long): F[Either[Models.ApiError, Models.Tread]] =
    getSomething(id, CollectionsNameEnum.TREADS)

  override def getTreads(boardId: Option[Long]): F[List[Models.Tread]] = boardId match {
    case None => getListOfSomething(CollectionsNameEnum.TREADS, None, None)
    case Some(id) => getListOfSomething(CollectionsNameEnum.TREADS, Some(CollectionsNameEnum.BOARDS), Some(id))
  }

  override def deleteTread(id: Long): F[Unit] = deleteSomething(id, CollectionsNameEnum.TREADS)

  override def createTread(boardId: Long, name: String): F[Either[Models.ApiError, Tread]] =
    sql"""INSERT INTO treads (name, board_id)
          values ($name, $boardId)""".update
      .withUniqueGeneratedKeys[Tread]("id", "name", "last_msg_id", "board_id")
      .transact(xa)
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        ApiError(422, s"Name $name already taken")
      }
      .flatMap {
        case Right(note) => Applicative[F].pure(Right(note))
        case Left(value) => Applicative[F].pure(Left(value))
      }

  override def updatePost(
                           id: Long,
                           refRespIds: Option[List[Long]],
                           refFromIds: Option[List[Long]],
                           imageIds: Option[List[Long]],
                         ): F[Either[ApiError, Post]] = {
    val refResFr = refRespIds.map { rIds =>
      fr"references_responses=${rIds.toString}"
    }
    val refFromFr = refFromIds.map { rIds =>
      fr"references_from=${rIds.toString}"
    }
    val imageIdsFr = imageIds.map { imIds =>
      fr"image_ids=${imIds.toString}"
    }

    fr"UPDATE posts ${setOpt(refResFr, refFromFr, imageIdsFr)} ${idFilterFr(id)}".update
      .withUniqueGeneratedKeys[Post](
        "id",
        "image_ids",
        "text",
        "created_at",
        "references_responses",
        "references_from",
        "tread_id",
      )
      .attempt
      .transact(xa)
      .handleError {
        case erId: InvariantViolation => Left(erId)
        case other => Left(other)
      }
      .map {
        case Left(_: InvariantViolation) => Left(ApiError(404, s"Note with id=$id not found"))
        case Left(_) => Left(ApiError(500, "Unexpected error"))
        case Right(post) => Right(post)
      }
  }

  override def updateTrad(id: Long, lastPostId: Long): F[Tread] =
    (fr"UPDATE treads" ++ set(fr"last_msg_id=$lastPostId")).update
      .withUniqueGeneratedKeys[Tread]("id", "name", "last_msg_id", "board_id")
      .transact(xa)

  override def createImage(path: String, postId: Long): F[Image] = sql"""INSERT INTO images (path, post_id) values ($path, $postId)""".update
    .withUniqueGeneratedKeys[Image]("id", "path", "post_id")
    .transact(xa)

  override def createReference(text: String, postId: Long, referenceTo: Long): F[ReferenceResponse] = sql"""INSERT INTO post_references (reference_to, post_id, text)
          values ($referenceTo, $postId, $text)""".update
    .withUniqueGeneratedKeys[ReferenceResponse]("id", "reference_to", "post_id", "text")
    .transact(xa)
}
