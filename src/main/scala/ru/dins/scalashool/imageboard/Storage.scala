package ru.dins.scalashool.imageboard

import ru.dins.scalashool.imageboard.models.ApiError
import ru.dins.scalashool.imageboard.models.DataBaseModels._

trait Storage[F[_]] {

  def createTopic(board: Long, name: String): F[Either[ApiError, TopicDB]]

  def getBoardWithTopic(id: Long): F[Either[ApiError, List[BoardWithTopicDB]]]

  def getBoards: F[List[BoardDB]]

  def createBoard(name: String): F[Either[ApiError, BoardDB]]

  def getEnrichedTopic(id: Long): F[Either[ApiError, List[EnrichedTopicDB]]]

  def deleteTopic(topicId: Long): F[Unit]

  def getCountPostsById(ids: List[Long]): F[Int]

  def createPostTransaction(
      topicId: Long,
      text: String,
      refs: List[(Long, String)],
      imagesPath: List[String],
  ): F[(PostDB, Int, Int)]
}
