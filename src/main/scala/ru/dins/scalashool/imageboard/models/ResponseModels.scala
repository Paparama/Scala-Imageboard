package ru.dins.scalashool.imageboard.models

import sttp.model.Part

import java.io.File
import java.time.LocalDateTime

object ResponseModels {

  case class PostResponse(
      id: Long,
      text: String,
      createdAt: LocalDateTime,
      images: List[ImageResponse],
      referencesTo: List[ReferenceResponse],
      referencesFrom: List[ReferenceResponse],
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
      id: Long,
      name: String,
      posts: List[PostResponse],
  )

  case class ListOfBoardsResponse(
      boards: List[BoardAtListResponse],
  )

  case class BoardAtListResponse(
      id: Long,
      name: String,
  )

  case class TopicAtListResponse(
      id: Long,
      name: String,
  )

  case class BoardResponse(
      id: Long,
      name: String,
      topics: List[TopicAtListResponse],
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
