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
      imageIds: List[Long],
      text: String,
      createdAt: LocalDateTime,
      referencesResponses: List[Long],
      referencesFrom: List[Long],
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
      boardId: Long,
      lastCreatedMsgTime: Option[LocalDateTime],
  )

  case class BoardDB(
      id: Long,
      name: String,
      topic_ids: List[Long]
  )

  case class BoardWithTopicDB(
      id: Long,
      name: String,
      topic_id: Option[Long],
      topic_name: Option[String],
  )

}
