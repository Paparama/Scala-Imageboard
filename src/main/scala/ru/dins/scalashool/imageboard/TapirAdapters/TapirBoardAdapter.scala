package ru.dins.scalashool.imageboard.TapirAdapters

import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.ResponseModels.{
  BoardCreateBody,
  BoardResponse,
  ListOfBoardsResponse,
  SuccessCreation,
}
import ru.dins.scalashool.imageboard.models.{ApiError, ModelConverter}

trait TapirBoardAdapter[F[_]] {
  def getBoard(id: Long): F[Either[ApiError, BoardResponse]]
  def getBoards: F[Either[ApiError, ListOfBoardsResponse]]
  def addBoard(body: BoardCreateBody): F[Either[ApiError, SuccessCreation]]
}

object TapirBoardAdapter {
  def apply[F[_]: Sync](storage: PostgresStorage[F], modelConverter: ModelConverter[F]) = new TapirBoardAdapter[F] {
    override def getBoard(id: Long): F[Either[ApiError, BoardResponse]] = storage.getBoardWithTopic(id).flatMap {
      case Left(error)    => Sync[F].delay(Left(error))
      case Right(boardDB) => Sync[F].delay(Right(modelConverter.convertBoardWithTopicToBoardResponse(boardDB)))
    }

    override def addBoard(body: BoardCreateBody): F[Either[ApiError, SuccessCreation]] =
      storage.createBoard(body.name).flatMap {
        case Left(error)  => Sync[F].delay(Left(error))
        case Right(board) => Sync[F].delay(Right(SuccessCreation(s"Board with name ${board.name} and id ${board.id} was created")))
      }

    override def getBoards: F[Either[ApiError, ListOfBoardsResponse]] = storage.getBoards.flatMap { boardList =>
      Sync[F].delay(modelConverter.convertBoardListDBToResponseListOfBoards(boardList).asRight)
    }
  }
}
