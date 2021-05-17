package ru.dins.scalashool.imageboard.db

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.postgres.sqlstate
import doobie.postgres.implicits._
import doobie.util.Read
import doobie.util.fragment.Fragment
import doobie.util.fragments.{set, setOpt, whereAnd, whereAndOpt}
import doobie.util.invariant.InvariantViolation
import doobie.util.transactor.Transactor.Aux
import ru.dins.scalashool.imageboard.Storage
import ru.dins.scalashool.imageboard.models.DataBaseModels
import ru.dins.scalashool.imageboard.models.DataBaseModels._
import ru.dins.scalashool.imageboard.models.HttpModels.ApiError

case class PostgresStorage[F[_]: Sync](xa: Aux[F, Unit]) extends Storage[F] {

  def idFilterFr(id: Long) = whereAnd(fr"""id = $id""")

  def collectionMapping(collection: CollectionsNameEnum.Value): Fragment = collection match {
    case CollectionsNameEnum.POSTS      => fr"posts"
    case CollectionsNameEnum.TOPICS     => fr"treads"
    case CollectionsNameEnum.BOARDS     => fr"boards"
    case CollectionsNameEnum.IMAGES     => fr"images"
    case CollectionsNameEnum.REFERENCES => fr"post_references"
  }

  def foreignKeyMapping(collection: CollectionsNameEnum.Value): Fragment = collection match {
    case CollectionsNameEnum.POSTS      => fr"post_id"
    case CollectionsNameEnum.TOPICS     => fr"tread_id"
    case CollectionsNameEnum.BOARDS     => fr"board_id"
  }

  def getSomething[A: Read](id: Long, collection: CollectionsNameEnum.Value): F[Either[ApiError, A]] = {
    val collectionsName = collectionMapping(collection)
    (fr"SELECT * FROM" ++ collectionsName ++ idFilterFr(id))
      .query[A]
      .option
      .transact(xa)
      .flatMap {
        case None        => Applicative[F].pure(Left(ApiError(404, s"There is no $collection with id=$id")))
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
    val mainCollection = collectionMapping(collection)

    val foreignKeyFilter =  whereAndOpt(filterId.flatMap { fid => filter.map { fc => fr"$fid = " ++ foreignKeyMapping(fc) } })

    (fr"SELECT * FROM " ++ mainCollection ++ foreignKeyFilter)
      .query[A]
      .to[List]
      .transact(xa)
  }

  override def getPost(id: Long): F[Either[ApiError, PostDB]] = getSomething(id, CollectionsNameEnum.POSTS)

  override def getPosts(treadId: Option[Long]): F[List[PostDB]] = treadId match {
    case None     => getListOfSomething(CollectionsNameEnum.POSTS, None, None)
    case Some(id) => getListOfSomething(CollectionsNameEnum.POSTS, Some(CollectionsNameEnum.TOPICS), Some(id))
  }

  override def deletePost(id: Long): F[Unit] = deleteSomething(id, CollectionsNameEnum.POSTS)

  override def createPost(
      treadId: Long,
      text: String,
      references: Option[List[Long]],
      imageIds: Option[List[Long]],
  ): F[PostDB] = {
    val finalImageIds = imageIds match {
      case None => List[Long]()
      case Some(ids) => ids
    }

    val finalRefIds = references match {
      case None => List[Long]()
      case Some(ids) => ids
    }

    val refFrom = List[Long]()
    sql"""INSERT INTO posts (image_ids, text, created_at, references_responses, tread_id, references_from)
          values ($finalImageIds, $text, current_timestamp, $finalRefIds, $treadId, $refFrom)""".update
      .withUniqueGeneratedKeys[PostDB](
        "id",
        "image_ids",
        "text",
        "created_at",
        "references_responses",
        "references_from",
        "tread_id",
      )
      .transact(xa)
  }

  override def getTopic(id: Long): F[Either[ApiError, TopicDB]] =
    getSomething(id, CollectionsNameEnum.TOPICS)

  override def getTopics(boardId: Option[Long]): F[List[DataBaseModels.TopicDB]] = boardId match {
    case None     => getListOfSomething(CollectionsNameEnum.TOPICS, None, None)
    case Some(id) => getListOfSomething(CollectionsNameEnum.TOPICS, Some(CollectionsNameEnum.BOARDS), Some(id))
  }

  override def deleteTopic(id: Long): F[Unit] = deleteSomething(id, CollectionsNameEnum.TOPICS)

  override def createTopic(boardId: Long, name: String): F[Either[ApiError, TopicDB]] =
    sql"""INSERT INTO treads (name, board_id)
          values ($name, $boardId)""".update
      .withUniqueGeneratedKeys[TopicDB]("id", "name", "last_msg_created_time", "board_id")
      .transact(xa)
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        ApiError(422, s"Name $name already taken")
      }
      .flatMap {
        case Right(tread) => Applicative[F].pure(Right(tread))
        case Left(value)  => Applicative[F].pure(Left(value))
      }

  override def updatePost(
      id: Long,
      refRespIds: Option[List[Long]],
      refFromIds: Option[List[Long]],
      imageIds: Option[List[Long]],
  ): F[Either[ApiError, PostDB]] = {
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
      .withUniqueGeneratedKeys[PostDB](
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
        case other                    => Left(other)
      }
      .map {
        case Left(_: InvariantViolation) => Left(ApiError(404, s"Post with id=$id not found"))
        case Left(_)                     => Left(ApiError(500, "Unexpected error"))
        case Right(post)                 => Right(post)
      }
  }

  override def updateTopic(id: Long, lastPostId: Long): F[TopicDB] =
    (fr"UPDATE treads" ++ set(fr"last_msg_id=$lastPostId")).update
      .withUniqueGeneratedKeys[TopicDB]("id", "name", "last_msg_id", "board_id")
      .transact(xa)

  override def createImage(path: String, postId: Long): F[ImageDB] =
    sql"""INSERT INTO images (path, post_id) values ($path, $postId)""".update
      .withUniqueGeneratedKeys[ImageDB]("id", "path", "post_id")
      .transact(xa)

  override def createReference(text: String, postId: Long, referenceTo: Long): F[ReferenceResponseDB] =
    sql"""INSERT INTO post_references (reference_to, post_id, text)
          values ($referenceTo, $postId, $text)""".update
      .withUniqueGeneratedKeys[ReferenceResponseDB]("id", "reference_to", "post_id", "text")
      .transact(xa)

  override def getImage(id: Long): F[Either[ApiError, ImageDB]] = getSomething(id, CollectionsNameEnum.IMAGES)

  override def getImagesBelongToPost(postId: Long): F[List[ImageDB]] = getListOfSomething(CollectionsNameEnum.IMAGES, Some(CollectionsNameEnum.POSTS), Some(postId))

  override def getReference(id: Long): F[Either[ApiError, ReferenceResponseDB]] =
    getSomething(id, CollectionsNameEnum.REFERENCES)

  override def getReferencesBelongToPost(postId: Long): F[Either[ApiError, List[ReferenceResponseDB]]] =
    (sql"SELECT * FROM post_references " ++ fr"WHERE $postId = post_id")
      .query[ReferenceResponseDB]
      .to[List]
      .transact(xa)
      .map(_.asRight)
      .map {
        case Left(_: InvariantViolation) => Left(ApiError(404, s"Post with id=$postId not found"))
        case Left(_)                     => Left(ApiError(500, "Unexpected error"))
        case Right(refs)                 => Right(refs)
      }

  override def getReferencesAnswerToPost(postId: Long): F[Either[ApiError, List[ReferenceResponseDB]]] =
    (sql"SELECT * FROM post_references "  ++ whereAnd(fr"$postId = reference_to"))
      .query[ReferenceResponseDB]
      .to[List]
      .transact(xa)
      .map(_.asRight)
      .map {
        case Left(_: InvariantViolation) => Left(ApiError(404, s"Post with id=$postId not found"))
        case Left(_)                     => Left(ApiError(500, "Unexpected error"))
        case Right(refs)                 => Right(refs)
      }

  override def deleteImage(id: Long): F[Unit] = deleteSomething(id, CollectionsNameEnum.IMAGES)

  override def deleteReference(id: Long): F[Unit] = deleteSomething(id, CollectionsNameEnum.REFERENCES)

  override def getBoard(id: Long): F[Either[ApiError, BoardDB]] = getSomething(id, CollectionsNameEnum.BOARDS)

  override def createBoard(name: String): F[Either[ApiError, BoardDB]] =
    sql"""INSERT INTO boards (name)
          values ($name)""".update
    .withUniqueGeneratedKeys[BoardDB]("id", "name")
    .transact(xa)
    .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
      ApiError(422, s"Name $name already taken")
    }
    .flatMap {
      case Right(tread) => Applicative[F].pure(Right(tread))
      case Left(value)  => Applicative[F].pure(Left(value))
    }

  override def deleteBoard(id: Long): F[Unit] = deleteSomething(id, CollectionsNameEnum.BOARDS)

}
