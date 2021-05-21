package ru.dins.scalashool.imageboard.db

import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.postgres.sqlstate
import doobie.postgres.implicits._
import doobie.util.fragments.whereAnd
import doobie.util.transactor.Transactor.Aux
import doobie.util.update.Update
import ru.dins.scalashool.imageboard.Storage
import ru.dins.scalashool.imageboard.models.{ApiError, NotFound, UnprocessableEntity}
import ru.dins.scalashool.imageboard.models.DataBaseModels._


case class PostgresStorage[F[_]: Sync](xa: Aux[F, Unit]) extends Storage[F] {

  def idFilterFr(id: Long) = whereAnd(fr"""id = $id""")

  override def createTopic(boardId: Long, name: String): F[Either[ApiError, TopicDB]] =
    sql"""INSERT INTO topics (name, board_id)
          values ($name, $boardId)""".update
      .withUniqueGeneratedKeys[TopicDB]("id", "name", "board_id", "last_msg_created_time")
      .transact(xa)
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        UnprocessableEntity(s"Name $name already taken")
      case sqlstate.class23.CHECK_VIOLATION =>
        UnprocessableEntity(s"Name should contain from 3 to 20 letters")
      case sqlstate.class23.FOREIGN_KEY_VIOLATION => UnprocessableEntity(s"Wrong board ID")
      }
      .flatMap {
        case Right(tread) => Sync[F].delay(Right(tread))
        case Left(value)  => Sync[F].delay(Left(value))
      }

  override def createPostTransaction(
      topicId: Long,
      text: String,
      refs: List[(Long, String)],
      imagesPath: List[String],
  ): F[Either[ApiError,PostDB]] = (for {
    post <- sql"""INSERT INTO posts (text, topic_id, created_at) values ($text, $topicId, current_timestamp)""".update
      .withUniqueGeneratedKeys[PostDB]("id", "text", "created_at", "topic_id")
    _ <- Update[(String, Long)]("""INSERT INTO images (path, post_id) values (?, ?)""").updateMany(
      imagesPath.map(it => (it, post.id)),
    )
    _ <- Update[(Long, String, Long)](
      """INSERT INTO post_references (reference_to, text, post_id) values (?, ?, ?)""",
    ).updateMany(refs.collect(it => (it._1, it._2, post.id)))
    _ <- sql"""UPDATE topics SET last_msg_created_time = ${post.createdAt} WHERE id = ${post.topicId}""".update
      .withUniqueGeneratedKeys[TopicDB]("id", "name", "board_id", "last_msg_created_time")
  } yield post).transact(xa).attemptSomeSqlState { case sqlstate.class23.FOREIGN_KEY_VIOLATION =>
    UnprocessableEntity(s"Wrong topic ID")
  case sqlstate.class23.CHECK_VIOLATION =>
    UnprocessableEntity(s"reference should contain from 3 to 1000 letters")
  }      .flatMap {
    case Right(postDB) => Sync[F].delay(Right(postDB))
    case Left(er)  => Sync[F].delay(Left(er))
  }

  def getPostCount(topicId: Long): F[Int] = fr"SELECT COUNT(*) FROM posts JOIN topics t on posts.topic_id = t.id WHERE posts.topic_id = $topicId".query[Int].unique.transact(xa)

  override def getBoardWithTopic(id: Long): F[Either[ApiError, List[BoardWithTopicDB]]] =
    (fr"SELECT b.id, b.name, t.id, t.name" ++
      fr" FROM boards b" ++
      fr" LEFT JOIN topics t" ++
      fr" ON b.id = t.board_id "
      ++ whereAnd(fr"b.id=$id") ++ fr"ORDER BY t.last_msg_created_time DESC NULLS LAST")
      .query[BoardWithTopicDB]
      .to[List]
      .transact(xa)
      .map {
        case Nil => Left(NotFound(s"Board with id $id not found"))
        case boards => Right(boards)
      }

  override def getBoards: F[List[BoardDB]] = sql"SELECT * FROM boards".query[BoardDB].to[List].transact(xa)

  override def createBoard(name: String): F[Either[ApiError, BoardDB]] =
    sql"""INSERT INTO boards (name)
          values ($name)""".update
      .withUniqueGeneratedKeys[BoardDB]("id", "name")
      .transact(xa)
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        UnprocessableEntity(s"Name $name already taken")
      case sqlstate.class23.CHECK_VIOLATION =>
        UnprocessableEntity(s"Name should contain from 3 to 20 letters")
      }
      .flatMap {
        case Right(tread) => Sync[F].delay(Right(tread))
        case Left(value)  => Sync[F].delay(Left(value))
      }

  override def getEnrichedTopic(id: Long): F[Either[ApiError, List[EnrichedTopicDB]]] =
    (fr"SELECT t.id, t.name, t.board_id, p.id, p.text, p.created_at, i.path, prFrom.text, prFrom.post_id, prTo.text, prTo.reference_to" ++
      fr" FROM topics t" ++
      fr"""    LEFT JOIN posts p
        |        ON t.id = p.topic_id
        |    LEFT JOIN images i on p.id = i.post_id
        |    LEFT JOIN post_references prTo on p.id = prTo.post_id
        |    LEFT JOIN post_references prFrom on p.id = prFrom.reference_to""".stripMargin
      ++ whereAnd(fr"t.id=$id") ++ fr"ORDER BY p.created_at DESC NULLS LAST")
      .query[EnrichedTopicDB]
      .to[List]
      .transact(xa)
      .map {
        case Nil => Left(NotFound(s"Topic with id $id not found"))
        case topics => Right(topics)
      }

  override def deleteTopic(topicId: Long): F[Unit] = (sql"DELETE FROM topics " ++ whereAnd(fr"id=$topicId")).update.run.transact(xa).void

  override def getCountPostsById(ids: List[Long]): F[Int] = sql"SELECT count(*) FROM posts WHERE id = ANY($ids)".query[Int].unique.transact(xa)

  override def createSubscribe(email: String, topicId: Long): F[Either[ApiError, Unit]] =
    sql"""INSERT INTO subscribers (email, topic_id)
         values ($email, $topicId)""".update.run
    .transact(xa)
    .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
      UnprocessableEntity(s"$email already get notifications")
    case sqlstate.class23.FOREIGN_KEY_VIOLATION => UnprocessableEntity(s"Wrong topic ID")
    }
    .flatMap {
      case Right(_) => Sync[F].delay(Right(()))
      case Left(value)  => Sync[F].delay(Left(value))
    }

  override def deleteSubscription(email: String, topicId: Long): F[Int] = (sql"DELETE FROM subscribers " ++ whereAnd(fr"email=$email", fr"topic_id=$topicId")).update.run.transact(xa)

  override def getSubscribers(topicId: Long): F[List[String]] = (sql"SELECT email FROM subscribers " ++ whereAnd(fr"topic_id=$topicId")).query[String].to[List].transact(xa)
}
