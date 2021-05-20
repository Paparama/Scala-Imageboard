package ru.dins.scalashool.imageboard.TapirAdapters

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.ResponseModels.{ApiError, BoardCreateBody, BoardResponse, ListOfBoardsResponse, SuccessCreation}
import ru.dins.scalashool.imageboard.models.ModelConverter

trait TapirBoardAdapter[F[_]] {
  def getBoard(id: Long): F[Either[ApiError, BoardResponse]]
  def getBoards(): F[Either[ApiError, ListOfBoardsResponse]]
  def addBoard(body: BoardCreateBody): F[Either[ApiError, SuccessCreation]]
  def deleteBoard(id: Long):  F[Either[ApiError, Unit]]
}

object TapirBoardAdapter {
  def apply[F[_] : Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F]) = new TapirBoardAdapter[F] {
    override def getBoard(id: Long): F[Either[ApiError, BoardResponse]] = storage.getBoardWithTopic(id).flatMap{
      case Left(error) => Applicative[F].pure(Left(error))
      case Right(boardDB) => Applicative[F].pure(Right(modelConverter.convertBoardWithTopicToBoardResponse(boardDB)))
    }

    override def addBoard(body: BoardCreateBody): F[Either[ApiError, SuccessCreation]] = storage.createBoard(body.name).flatMap{
      case Left(error) => Applicative[F].pure(Left(error))
      case Right(board) =>  Applicative[F].pure(Right(SuccessCreation(s"Board with name ${board.name} was created")))
    }

    override def deleteBoard(id: Long): F[Either[ApiError, Unit]] = storage.deleteBoard(id).map(_.asRight)

    override def getBoards(): F[Either[ApiError, ListOfBoardsResponse]] = storage.getBoards().flatMap { boardList =>
    Applicative[F].pure(modelConverter.convertBoardListDBToResponseListOfBoards(boardList).asRight)
    }
  }
}
