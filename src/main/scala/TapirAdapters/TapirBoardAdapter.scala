package TapirAdapters

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.HttpModels.{ApiError, BoardCreateBody, BoardHttp}
import ru.dins.scalashool.imageboard.models.ModelConverter

trait TapirBoardAdapter[F[_]] {
  def getBoard(id: Long): F[Either[ApiError, BoardHttp]]
  def addBoard(body: BoardCreateBody): F[Either[ApiError, BoardHttp]]
  def deleteBoard(id: Long):  F[Either[ApiError, Unit]]
}

object TapirBoardAdapter {
  def apply[F[_] : Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F]) = new TapirBoardAdapter[F] {
    override def getBoard(id: Long): F[Either[ApiError, BoardHttp]] = storage.getBoard(id).flatMap{
      case Left(error) => Applicative[F].pure(Left(error))
      case Right(boardDB) => Applicative[F].pure(Right(modelConverter.convertBoard(boardDB)))
    }

    override def addBoard(body: BoardCreateBody): F[Either[ApiError, BoardHttp]] = storage.createBoard(body.name).flatMap{
      case Left(error) => Applicative[F].pure(Left(error))
      case Right(boardDB) =>  Applicative[F].pure(Right(modelConverter.convertBoard(boardDB)))
    }

    override def deleteBoard(id: Long): F[Either[ApiError, Unit]] = storage.deleteBoard(id).map(_.asRight)
  }
}
