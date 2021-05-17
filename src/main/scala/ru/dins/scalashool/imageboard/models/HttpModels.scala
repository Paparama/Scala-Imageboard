package ru.dins.scalashool.imageboard.models

import java.time.LocalDateTime

object HttpModels {

  case class PostHttp(
      images: List[ImageHttp],
      text: String,
      createdAt: LocalDateTime,
      referencesResponses: List[ReferenceResponseHttp],
      referencesFrom: List[ReferenceResponseHttp],
      treadName: String,
  )

  case class ImageHttp(
      path: String,
  )

  case class TopicHttp(
      name: String,
      posts: List[PostHttp],
      boardName: String,
  )

  case class BoardHttp(
      name: String,
  )

  case class ReferenceResponseHttp(
      postId: Long,
      text: String,
  )

  case class ApiError(code: Int, message: String)

  case class PostCreationBody(treadId: Long, text: String, references: Option[List[Long]], imageIds: Option[List[Long]])
  case class PostUpdateBody(
      refRespIds: Option[List[Long]],
      refFromIds: Option[List[Long]],
      imageIds: Option[List[Long]],
  )

  case class TopicCreationBody(boardId: Long, name: String)
  case class TopicUpdateBody(lastMessageId: Long)
  case class ReferenceCreateBody(postId: Long, text: String, referenceTo: Long)
  case class BoardCreateBody(name: String)

}
