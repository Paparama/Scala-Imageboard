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

  case class PostCreationBody(topicId: Long, text: String, images: Part[List[File]])

  case class TopicCreationBody(boardId: Long, name: String)
  case class BoardCreateBody(name: String)

}
