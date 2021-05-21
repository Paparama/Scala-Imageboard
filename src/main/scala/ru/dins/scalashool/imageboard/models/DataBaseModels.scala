package ru.dins.scalashool.imageboard.models

import java.time.LocalDateTime

object DataBaseModels {

  case class ReferenceDB(
      id: Long,
      referenceTo: Long,
      text: String,
      postId: Long,
  )

  case class ReferenceToDB(
      referenceTo: Long,
      text: String,
  )

  case class ReferenceFromDB(
      postId: Long,
      text: String,
  )

  case class PostDB(
                     id: Long,
                     text: String,
                     createdAt: LocalDateTime,
                     topicId: Long,
  )

  case class EnrichedPostDB(
                             id: Long,
                             imageIds: List[Long],
                             text: String,
                             createdAt: LocalDateTime,
                             referencesResponses: List[ReferenceToDB],
                             referencesFrom: List[ReferenceFromDB],
  )

  case class ImageDB(
      id: Long,
      path: String,
      postId: Long,
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
