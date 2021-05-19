package ru.dins.scalashool.imageboard.models

import sttp.model.Part

import java.io.File
import java.time.LocalDateTime

object ResponseModels {

  case class PostResponse(
      images: List[ImageResponse],
      text: String,
      createdAt: LocalDateTime,
      referencesResponses: List[ReferenceResponse],
      referencesFrom: List[ReferenceResponse],
      treadName: String,
  )

  case class ImageResponse(
      path: String,
  )

  case class SuccessCreation(
      result: String,
  )

  case class SuccessUpdate(
      result: String,
  )

  case class TopicResponse(
      name: String,
      posts: List[PostResponse],
      boardName: String,
  )

  case class BoardResponse(
      name: String,
  )

  case class ReferenceResponse(
      postId: Long,
      text: String,
  )

  case class ApiError(code: Int, message: String)

  case class PostCreationBody(treadId: Long, text: String, references: List[Long], imageIds: List[Long])

  case class PostUpdateBody(
      refRespIds: Option[List[Long]],
      refFromIds: Option[List[Long]],
      imageIds: Option[List[Long]],
  )

  case class TopicCreationBody(boardId: Long, name: String)
  case class TopicUpdateBody(lastMessageId: Long)
  case class ReferenceCreateBody(postId: Long, text: String, referenceTo: Long)
  case class BoardCreateBody(name: String)

  case class ImageUpload(postId: Long, data: Part[File])

}
