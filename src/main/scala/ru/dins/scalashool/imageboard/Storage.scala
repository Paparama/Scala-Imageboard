package ru.dins.scalashool.imageboard

import ru.dins.scalashool.imageboard.models.DataBaseModels._
import ru.dins.scalashool.imageboard.models.ResponseModels.ApiError

trait Storage[F[_]] {

  def deletePost(id: Long): F[Unit]

  def createPost(tread: Long, text: String, references: List[Long], imageIds: List[Long]): F[PostDB]

  def getTopic(id: Long): F[Either[ApiError, TopicDB]]

  def getTopics(
      boardId: Option[Long],
  ): F[List[TopicDB]]

  def deleteTopic(id: Long): F[Unit]

  def createTopic(board: Long, name: String): F[Either[ApiError, TopicDB]]

  def createImage(path: String, postId: Long): F[ImageDB]

  def createReference(text: String, postId: Long, referenceTo: Long): F[ReferenceDB]

  def getImage(id: Long): F[Either[ApiError, ImageDB]]

  def getReference(id: Long):  F[Either[ApiError, ReferenceDB]]

  def getImagesBelongToPost(postId: Long): F[List[ImageDB]]

  def getReferencesBelongToPost(postId: Long):  F[Either[ApiError, List[ReferenceDB]]]

  def deleteImage(id: Long): F[Unit]

  def deleteReference(id: Long): F[Unit]

  def getBoardWithTopic(id: Long): F[Either[ApiError, List[BoardWithTopicDB]]]

  def getBoards(): F[List[BoardDB]]

  def createBoard(name: String): F[Either[ApiError, BoardDB]]

  def deleteBoard(id: Long): F[Unit]

  def getReferencesAnswerToPost(postId: Long): F[Either[ApiError, List[ReferenceDB]]]

  def getEnrichedTopic(id: Long): F[Either[ApiError, List[EnrichedTopicDB]]]
}
