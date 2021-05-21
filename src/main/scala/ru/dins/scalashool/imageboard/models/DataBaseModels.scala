package ru.dins.scalashool.imageboard.models

import java.time.LocalDateTime

object DataBaseModels {

  case class PostDB(
      id: Long,
      text: String,
      createdAt: LocalDateTime,
      topicId: Long,
  )

  case class EnrichedTopicDB(
      id: Long,
      name: String,
      boardId: Long,
      postId: Option[Long],
      postText: Option[String],
      postCreated: Option[LocalDateTime],
      imagePath: Option[String],
      refFromText: Option[String],
      refFromPostId: Option[Long],
      refToText: Option[String],
      refToPostId: Option[Long],
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
  )

  case class BoardWithTopicDB(
      id: Long,
      name: String,
      topicId: Option[Long],
      topicName: Option[String],
  )

}
