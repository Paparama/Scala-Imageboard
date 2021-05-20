package ru.dins.scalashool.imageboard.models

import cats.effect.Sync
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.DataBaseModels._
import ru.dins.scalashool.imageboard.models.ResponseModels._

trait ModelConverter[F[_]] {
  def convertImage(image: ImageDB): ImageResponse
  def convertReference(reference: ReferenceDB): ReferenceResponse
  def convertImages(images: List[ImageDB]): List[ImageResponse]
  def convertReferences(references: List[ReferenceDB]): List[ReferenceResponse]
  def convertBoardListDBToResponseListOfBoards(boardsDB: List[BoardDB]): ListOfBoardsResponse
  def convertBoardWithTopicToBoardResponse(boardsDB: List[BoardWithTopicDB]): BoardResponse
  def convertEnrichedTopicsToResponse(topics: List[EnrichedTopicDB]): TopicResponse
}

object ModelConverter {
  def apply[F[_]: Sync](storage: PostgresStorage[F]) = new ModelConverter[F] {

    override def convertImage(image: ImageDB): ImageResponse = image match {
      case ImageDB(_, path, _) => ImageResponse(path)
    }

    override def convertImages(images: List[ImageDB]): List[ImageResponse] = images.map(convertImage)

    override def convertReferences(references: List[ReferenceDB]): List[ReferenceResponse] =
      references.map(convertReference)

    override def convertReference(reference: ReferenceDB): ReferenceResponse = reference match {
      case ReferenceDB(_, _, text, postId) => ReferenceResponse(postId, text)
    }

    override def convertBoardListDBToResponseListOfBoards(boardsDB: List[BoardDB]): ListOfBoardsResponse = {
      val listOfBoards: List[BoardAtListResponse] = boardsDB.collect(it => BoardAtListResponse(it.id, it.name))
      ListOfBoardsResponse(listOfBoards)
    }
    override def convertBoardWithTopicToBoardResponse(boardsDB: List[BoardWithTopicDB]): BoardResponse = {
      val ListOfTopics = boardsDB collect { case BoardWithTopicDB(_, _, Some(tId), Some(tName)) =>
        TopicAtListResponse(tId, tName)
      }
      BoardResponse(boardsDB.head.id, boardsDB.head.name, ListOfTopics)
    }

    override def convertEnrichedTopicsToResponse(topics: List[EnrichedTopicDB]): TopicResponse = {

      def getImageByPostId(postId: Long, enrichedTopicsDB: List[EnrichedTopicDB]): List[ImageResponse] = (enrichedTopicsDB collect {
        case EnrichedTopicDB(id, name, boardId, Some(pId), postText, postCreated, Some(imagePath), refFromText, refFromPostId, refToText, refToPostId) if pId == postId => ImageResponse(imagePath)
      }).distinct

      def getRefToByPostId(postId: Long, enrichedTopicsDB: List[EnrichedTopicDB]): List[ReferenceResponse] = (enrichedTopicsDB collect {
        case EnrichedTopicDB(id, name, boardId, Some(pId), postText, postCreated, imagePath, refFromText, refFromPostId, Some(refToText), Some(refToPostId)) if pId == postId => ReferenceResponse(refToPostId, refToText)
      }).distinct

      def getRefFromByPostId(postId: Long, enrichedTopicsDB: List[EnrichedTopicDB]): List[ReferenceResponse] = (enrichedTopicsDB collect {
        case EnrichedTopicDB(id, name, boardId, Some(pId), postText, postCreated, imagePath, Some(refFromText), Some(refFromPostId), refToText, refToPostId) if pId == postId => ReferenceResponse(refFromPostId, refFromText)
      }).distinct

      val postData = (topics collect {
        case EnrichedTopicDB(id, name, boardId, Some(postId), Some(postText), Some(postCreated), imagePath, refFromText, refFromPostId, refToText, refToPostId) => (postId, postText, postCreated)
      }).distinct

      TopicResponse(topics.head.id, topics.head.name, postData collect { it => PostResponse(it._1, it._2, it._3, getImageByPostId(it._1, topics), getRefToByPostId(it._1, topics), getRefFromByPostId(it._1, topics))
      })

    }
  }
}
