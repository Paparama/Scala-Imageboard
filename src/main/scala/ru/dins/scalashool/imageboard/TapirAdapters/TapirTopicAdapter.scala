package ru.dins.scalashool.imageboard.TapirAdapters

import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.ResponseModels.{SuccessCreation, TopicCreationBody, TopicResponse}
import ru.dins.scalashool.imageboard.models.{ApiError, ModelConverter}

trait TapirTopicAdapter[F[_]] {

  def getTopic(id: Long): F[Either[ApiError, TopicResponse]]

  def addTopic(body: TopicCreationBody): F[Either[ApiError, SuccessCreation]]

}

object TapirTopicAdapter {
  def apply[F[_]: Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F]) = new TapirTopicAdapter[F] {
    override def getTopic(id: Long): F[Either[ApiError, TopicResponse]] =
      storage.getEnrichedTopic(id).flatMap {
        case Left(error)    => Sync[F].delay(Left(error))
        case Right(topicDB) => Sync[F].delay(Right(modelConverter.convertEnrichedTopicsToResponse(topicDB)))
      }

    override def addTopic(body: TopicCreationBody): F[Either[ApiError, SuccessCreation]] = body match {
      case TopicCreationBody(boardId, name) =>
        storage.createTopic(boardId, name).flatMap {
          case Left(error) => Sync[F].delay(Left(error))
          case Right(topicDB) =>
            Sync[F].delay(Right(SuccessCreation(s"Topic with name ${topicDB.name} and id ${topicDB.id} was created")))
        }
    }
  }
}
