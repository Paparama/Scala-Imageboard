package ru.dins.scalashool.imageboard

import ru.dins.scalashool.imageboard.models.Models._

trait Storage[F[_]] {
  def getPost(id: Long): F[Either[ApiError, Post]]

  def getPosts(
      treadId: Option[Long],
  ): F[List[Post]]

  def deletePost(id: Long): F[Unit]

  def createPost(tread: Long, text: String, references: Option[List[Long]], imageIds: Option[List[Long]]): F[Post]

  def updatePost(
      id: Long,
      refRespIds: Option[List[Long]],
      refFromIds: Option[List[Long]],
      imageIds: Option[List[Long]],
  ): F[Either[ApiError, Post]]

  def getTread(id: Long): F[Either[ApiError, Tread]]

  def getTreads(
      boardId: Option[Long],
  ): F[List[Tread]]

  def deleteTread(id: Long): F[Unit]

  def createTread(board: Long, name: String): F[Either[ApiError, Tread]]

  def updateTrad(id: Long, lastPostId: Long): F[Tread]

  def createImage(path: String, postId: Long): F[Image]

  def createReference(text: String, postId: Long, referenceTo: Long): F[ReferenceResponse]

}
