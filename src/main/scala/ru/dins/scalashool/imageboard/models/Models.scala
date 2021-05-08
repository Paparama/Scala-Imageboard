package ru.dins.scalashool.imageboard.models

import java.time.LocalDateTime

object Models {

  case class ReferenceResponse(
      id: Long,
      referenceTo: Long,
      text: String,
      postId: Long,
  )

  case class Post(
      id: Long,
      imageIds: Option[List[Long]],
      text: String,
      createdAt: LocalDateTime,
      referencesResponses: Option[List[Long]],
      referencesFrom: Option[List[Long]],
      treadId: Long,
  )

  case class Image(
      id: Long,
      path: String,
      postId: Long,
  )

  case class Tread(
      id: Long,
      name: String,
      lastMsgId: Option[Long],
      boardId: Long,
  )

  case class Board(
      id: Long,
      name: String,
  )

  case class ApiError(code: Int, message: String)

}
