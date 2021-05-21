package ru.dins.scalashool.imageboard.db

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.postgres.sqlstate
import doobie.postgres.implicits._
import doobie.util.Read
import doobie.util.fragment.Fragment
import doobie.util.fragments.{whereAnd, whereAndOpt}
import doobie.util.invariant.InvariantViolation
import doobie.util.transactor.Transactor.Aux
import doobie.util.update.Update
import ru.dins.scalashool.imageboard.Storage
import ru.dins.scalashool.imageboard.models.DataBaseModels
import ru.dins.scalashool.imageboard.models.DataBaseModels._
import ru.dins.scalashool.imageboard.models.ResponseModels.ApiError

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

  override def deletePost(id: Long): F[Unit] = deleteSomething(id, CollectionsNameEnum.POSTS)

  override def createPost(
      treadId: Long,
      text: String,
      references: List[Long],
      imageIds: List[Long],
  ): F[PostDB] = {

    sql"""INSERT INTO posts (text, created_at, topic_id)
          values ($text, current_timestamp, $treadId)""".update
      .withUniqueGeneratedKeys[PostDB](
        "id",
        "text",
        "created_at",
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
    sql"""INSERT INTO topics (name, board_id)
          values ($name, $boardId)""".update
      .withUniqueGeneratedKeys[TopicDB]("id", "name", "board_id", "last_msg_created_time")
      .transact(xa)
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        ApiError(422, s"Name $name already taken")
      }
      .flatMap {
        case Right(tread) => Applicative[F].pure(Right(tread))
        case Left(value)  => Applicative[F].pure(Left(value))
      }

  override def createPostTransaction(      topicId: Long,
                                  text: String,
                                  refs: List[(Long, String)],
                                  imagesPath: List[String]): F[(PostDB, Int, Int)] = (for {
    post <- sql"""INSERT INTO posts (text, topic_id, created_at) values ($text, $topicId, current_timestamp)""".update.withUniqueGeneratedKeys[PostDB]("id", "text", "created_at", "topic_id")
    images <- Update[(String, Long)]("""INSERT INTO images (path, post_id) values (?, ?)""").updateMany(imagesPath.zipAll(List(post.id), "", post.id))
    refsDB <- Update[(Long, String, Long)]("""INSERT INTO post_references (reference_to, text, post_id) values (?, ?, ?)""").updateMany(refs.collect { it => (it._1, it._2, post.id)})
    _ <- sql"""UPDATE topics SET last_msg_created_time = ${post.createdAt}""".update.withUniqueGeneratedKeys[TopicDB]("id", "name", "board_id", "last_msg_created_time")
  } yield (post, images, refsDB)).transact(xa)

  override def createImage(path: String, postId: Long): F[ImageDB] =
    sql"""INSERT INTO images (path, post_id) values ($path, $postId)""".update
      .withUniqueGeneratedKeys[ImageDB]("id", "path", "post_id")
      .transact(xa)

  override def createReference(text: String, postId: Long, referenceTo: Long): F[ReferenceDB] =
    sql"""INSERT INTO post_references (reference_to, post_id, text)
          values ($referenceTo, $postId, $text)""".update
      .withUniqueGeneratedKeys[ReferenceDB]("id", "reference_to", "post_id", "text")
      .transact(xa)

  override def getImage(id: Long): F[Either[ApiError, ImageDB]] = getSomething(id, CollectionsNameEnum.IMAGES)

  override def getImagesBelongToPost(postId: Long): F[List[ImageDB]] = getListOfSomething(CollectionsNameEnum.IMAGES, Some(CollectionsNameEnum.POSTS), Some(postId))

  override def getReference(id: Long): F[Either[ApiError, ReferenceDB]] =
    getSomething(id, CollectionsNameEnum.REFERENCES)

  override def getReferencesBelongToPost(postId: Long): F[Either[ApiError, List[ReferenceDB]]] =
    (sql"SELECT * FROM post_references " ++ fr"WHERE $postId = post_id")
      .query[ReferenceDB]
      .to[List]
      .transact(xa)
      .map(_.asRight)
      .map {
        case Left(_: InvariantViolation) => Left(ApiError(404, s"Post with id=$postId not found"))
        case Left(_)                     => Left(ApiError(500, "Unexpected error"))
        case Right(refs)                 => Right(refs)
      }

  override def getReferencesAnswerToPost(postId: Long): F[Either[ApiError, List[ReferenceDB]]] =
    (sql"SELECT * FROM post_references "  ++ whereAnd(fr"$postId = reference_to"))
      .query[ReferenceDB]
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

  override def getBoardWithTopic(id: Long): F[Either[ApiError, List[BoardWithTopicDB]]] = (fr"SELECT b.id, b.name, t.id, t.name" ++
    fr" FROM boards b" ++
    fr" LEFT JOIN topics t" ++
    fr" ON b.id = t.board_id "
    ++ whereAnd(fr"b.id=$id") ++ fr"ORDER BY t.last_msg_created_time").query[BoardWithTopicDB]
    .to[List]
    .transact(xa)
    .map(_.asRight)
    .map {
      case Left(_: InvariantViolation) => Left(ApiError(404, s"Board with id=$id not found"))
      case Left(_)                     => Left(ApiError(500, "Unexpected error"))
      case Right(boards)                 => Right(boards)
    }

  override def getBoards(): F[List[BoardDB]] = sql"SELECT * FROM boards".query[BoardDB].to[List].transact(xa)

  override def createBoard(name: String): F[Either[ApiError, BoardDB]] = {
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
  }

  override def deleteBoard(id: Long): F[Unit] = deleteSomething(id, CollectionsNameEnum.BOARDS)

  override def getEnrichedTopic(id: Long): F[Either[ApiError, List[EnrichedTopicDB]]] = (fr"SELECT t.id, t.name, t.board_id, p.id, p.text, p.created_at, i.path, prFrom.text, prFrom.post_id, prTo.text, prTo.reference_to" ++
    fr" FROM topics t" ++
    fr"""    LEFT JOIN posts p
        |        ON t.id = p.topic_id
        |    LEFT JOIN images i on p.id = i.post_id
        |    LEFT JOIN post_references prTo on p.id = prTo.post_id
        |    LEFT JOIN post_references prFrom on p.id = prFrom.reference_to""".stripMargin
    ++ whereAnd(fr"t.id=$id")  ++ fr"ORDER BY p.created_at").query[EnrichedTopicDB]
    .to[List]
    .transact(xa)
    .map(_.asRight)
    .map {
      case Left(_: InvariantViolation) => Left(ApiError(404, s"Topic with id=$id not found"))
      case Left(_)                     => Left(ApiError(500, "Unexpected error"))
      case Right(topic)                 => Right(topic)
    }
}
