package ru.dins.scalashool.imageboard.db

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import doobie.implicits._
import doobie.postgres.sqlstate
import doobie.postgres.implicits._
import doobie.util.fragments.whereAnd
import doobie.util.invariant.InvariantViolation
import doobie.util.transactor.Transactor.Aux
import doobie.util.update.Update
import ru.dins.scalashool.imageboard.Storage
import ru.dins.scalashool.imageboard.models.DataBaseModels._
import ru.dins.scalashool.imageboard.models.ResponseModels.ApiError

case class PostgresStorage[F[_]: Sync](xa: Aux[F, Unit]) extends Storage[F] {

  def idFilterFr(id: Long) = whereAnd(fr"""id = $id""")

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

  override def createPostTransaction(
      topicId: Long,
      text: String,
      refs: List[(Long, String)],
      imagesPath: List[String],
  ): F[(PostDB, Int, Int)] = (for {
    post <- sql"""INSERT INTO posts (text, topic_id, created_at) values ($text, $topicId, current_timestamp)""".update
      .withUniqueGeneratedKeys[PostDB]("id", "text", "created_at", "topic_id")
    images <- Update[(String, Long)]("""INSERT INTO images (path, post_id) values (?, ?)""").updateMany(
      imagesPath.zipAll(List(post.id), "", post.id),
    )
    refsDB <- Update[(Long, String, Long)](
      """INSERT INTO post_references (reference_to, text, post_id) values (?, ?, ?)""",
    ).updateMany(refs.collect(it => (it._1, it._2, post.id)))
    _ <- sql"""UPDATE topics SET last_msg_created_time = ${post.createdAt}""".update
      .withUniqueGeneratedKeys[TopicDB]("id", "name", "board_id", "last_msg_created_time")
  } yield (post, images, refsDB)).transact(xa)

  override def getBoardWithTopic(id: Long): F[Either[ApiError, List[BoardWithTopicDB]]] =
    (fr"SELECT b.id, b.name, t.id, t.name" ++
      fr" FROM boards b" ++
      fr" LEFT JOIN topics t" ++
      fr" ON b.id = t.board_id "
      ++ whereAnd(fr"b.id=$id") ++ fr"ORDER BY t.last_msg_created_time")
      .query[BoardWithTopicDB]
      .to[List]
      .transact(xa)
      .map(_.asRight)
      .map {
        case Left(_: InvariantViolation) => Left(ApiError(404, s"Board with id=$id not found"))
        case Left(_)                     => Left(ApiError(500, "Unexpected error"))
        case Right(boards)               => Right(boards)
      }

  override def getBoards: F[List[BoardDB]] = sql"SELECT * FROM boards".query[BoardDB].to[List].transact(xa)

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

  override def getEnrichedTopic(id: Long): F[Either[ApiError, List[EnrichedTopicDB]]] =
    (fr"SELECT t.id, t.name, t.board_id, p.id, p.text, p.created_at, i.path, prFrom.text, prFrom.post_id, prTo.text, prTo.reference_to" ++
      fr" FROM topics t" ++
      fr"""    LEFT JOIN posts p
        |        ON t.id = p.topic_id
        |    LEFT JOIN images i on p.id = i.post_id
        |    LEFT JOIN post_references prTo on p.id = prTo.post_id
        |    LEFT JOIN post_references prFrom on p.id = prFrom.reference_to""".stripMargin
      ++ whereAnd(fr"t.id=$id") ++ fr"ORDER BY p.created_at")
      .query[EnrichedTopicDB]
      .to[List]
      .transact(xa)
      .map(_.asRight)
      .map {
        case Left(_: InvariantViolation) => Left(ApiError(404, s"Topic with id=$id not found"))
        case Left(_)                     => Left(ApiError(500, "Unexpected error"))
        case Right(topic)                => Right(topic)
      }
}
