package ru.dins.scalashool.imageboard

import ru.dins.scalashool.imageboard.models.DataBaseModels._
import ru.dins.scalashool.imageboard.models.HttpModels.ApiError

trait Storage[F[_]] {
  def getPost(id: Long): F[Either[ApiError, PostDB]]

  def getPosts(
      treadId: Option[Long],
  ): F[List[PostDB]]

  def deletePost(id: Long): F[Unit]

  def createPost(tread: Long, text: String, references: Option[List[Long]], imageIds: Option[List[Long]]): F[PostDB]

  def updatePost(
      id: Long,
      refRespIds: Option[List[Long]],
      refFromIds: Option[List[Long]],
      imageIds: Option[List[Long]],
  ): F[Either[ApiError, PostDB]]

  def getTopic(id: Long): F[Either[ApiError, TopicDB]]

  def getTopics(
      boardId: Option[Long],
  ): F[List[TopicDB]]

  def deleteTopic(id: Long): F[Unit]

  def createTopic(board: Long, name: String): F[Either[ApiError, TopicDB]]

  def updateTopic(id: Long, lastPostId: Long): F[TopicDB]

  def createImage(path: String, postId: Long): F[ImageDB]

  def createReference(text: String, postId: Long, referenceTo: Long): F[ReferenceResponseDB]

  def getImage(id: Long): F[Either[ApiError, ImageDB]]

  def getReference(id: Long):  F[Either[ApiError, ReferenceResponseDB]]

  def getImagesBelongToPost(postId: Long): F[List[ImageDB]]

  def getReferencesBelongToPost(postId: Long):  F[Either[ApiError, List[ReferenceResponseDB]]]

  def deleteImage(id: Long): F[Unit]

  def deleteReference(id: Long): F[Unit]

  def getBoard(id: Long): F[Either[ApiError, BoardDB]]

  def createBoard(name: String): F[Either[ApiError, BoardDB]]

  def deleteBoard(id: Long): F[Unit]

  def getReferencesAnswerToPost(postId: Long): F[Either[ApiError, List[ReferenceResponseDB]]]
}
