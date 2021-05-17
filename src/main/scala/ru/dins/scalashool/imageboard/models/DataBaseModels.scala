package ru.dins.scalashool.imageboard.models

import java.time.LocalDateTime

object DataBaseModels {

  case class ReferenceResponseDB(
      id: Long,
      referenceTo: Long,
      text: String,
      postId: Long,
  )

  case class PostDB(
      id: Long,
      imageIds: Option[List[Long]],
      text: String,
      createdAt: LocalDateTime,
      referencesResponses: Option[List[Long]],
      referencesFrom: Option[List[Long]],
      treadId: Long,
  )

  case class ImageDB(
      id: Long,
      path: String,
      postId: Long,
  )

  case class TopicDB(
      id: Long,
      name: String,
      lastCreatedMsgTime: Option[LocalDateTime],
      boardId: Long,
  )

  case class BoardDB(
      id: Long,
      name: String,
  )

}
