package ru.dins.scalashool.imageboard.TapirAdapters

import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.ResponseModels.{
  SubscribeCreateBody,
  SuccessCreation,
  SuccessUnsubscribe,
  UnsubscribeBody,
}
import ru.dins.scalashool.imageboard.models.{ApiError, NotFound, UnprocessableEntity}

trait TapirSubscribeAdapter[F[_]] {
  def deleteSubscribe(unsubscribeBody: UnsubscribeBody): F[Either[ApiError, SuccessUnsubscribe]]
  def addSubscribe(body: SubscribeCreateBody): F[Either[ApiError, SuccessCreation]]
}

object TapirSubscribeAdapter {
  def apply[F[_]: Sync](storage: PostgresStorage[F]) = new TapirSubscribeAdapter[F] {

    private def checkEmail(body: SubscribeCreateBody): Either[ApiError, String] = {
      val emailRegex =
        """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

      def check(e: String): Boolean = e match {
        case null                                          => false
        case e if e.trim.isEmpty                           => false
        case e if emailRegex.findFirstMatchIn(e).isDefined => true
        case _                                             => false
      }

      if (check(body.email)) Right(body.email)
      else Left(UnprocessableEntity("Wrong email"))
    }

    override def deleteSubscribe(unsubscribeBody: UnsubscribeBody): F[Either[ApiError, SuccessUnsubscribe]] =
      storage.deleteSubscription(unsubscribeBody.email, unsubscribeBody.topicId).flatMap {
        case 1 => Sync[F].delay(Right(SuccessUnsubscribe("Your email has been removed from mailing list")))
        case _ => Sync[F].delay(Left(NotFound("Not found email subscribed to topic")))
      }

    override def addSubscribe(body: SubscribeCreateBody): F[Either[ApiError, SuccessCreation]] = checkEmail(
      body,
    ) match {
      case Left(er) => Sync[F].delay(Left(er))
      case Right(email) =>
        storage.createSubscribe(email, body.topicId).flatMap {
          case Left(er) => Sync[F].delay(Left(er))
          case Right(_) => Sync[F].delay(Right(SuccessCreation("your email has been added to the mailing list")))
        }
    }

  }
}
