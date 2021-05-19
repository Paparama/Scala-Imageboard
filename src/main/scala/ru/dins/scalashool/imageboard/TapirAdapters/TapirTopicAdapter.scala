package ru.dins.scalashool.imageboard.TapirAdapters

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.ResponseModels.{ApiError, TopicResponse, TopicCreationBody, TopicUpdateBody}
import ru.dins.scalashool.imageboard.models.ModelConverter

trait TapirTopicAdapter[F[_]] {

  def getTread(id: Long): F[Either[ApiError, TopicResponse]]

  def addTread(body: TopicCreationBody): F[Either[ApiError, TopicResponse]]

  def updateTread(idAndBody: (Long, TopicUpdateBody)): F[Either[ApiError, TopicResponse]]

  def deleteTread(id: Long): F[Either[ApiError, Unit]]

}

object TapirTopicAdapter {
  def apply[F[_] : Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F]) = new TapirTopicAdapter[F] {
    override def getTread(id: Long): F[Either[ApiError, TopicResponse]] =
      storage.getTopic(id).flatMap{
        case Left(error) => Applicative[F].pure(Left(error))
        case Right(topicDB) => modelConverter.convertTopic(topicDB)
      }

    override def addTread(body: TopicCreationBody): F[Either[ApiError, TopicResponse]] = body match {
      case TopicCreationBody(boardId, name) => storage.createTopic(boardId,name).flatMap{
        case Left(error) => Applicative[F].pure(Left(error))
        case Right(topicDB) => modelConverter.convertTopic(topicDB)
      }
    }

    override def updateTread(idAndBody: (Long, TopicUpdateBody)): F[Either[ApiError, TopicResponse]] = idAndBody._2 match {
      case TopicUpdateBody(lastMessageId) => storage.updateTopic(idAndBody._1, lastMessageId).flatMap(modelConverter.convertTopic)
    }

    override def deleteTread(id: Long): F[Either[ApiError, Unit]] = storage.deleteTopic(id).map(_.asRight)
  }
}


