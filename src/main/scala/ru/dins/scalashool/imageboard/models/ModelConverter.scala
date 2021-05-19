package ru.dins.scalashool.imageboard.models

import cats.Applicative
import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.DataBaseModels._
import ru.dins.scalashool.imageboard.models.ResponseModels._

trait ModelConverter[F[_]] {
  def convertPost(post: PostDB): F[Either[ApiError, PostResponse]]
  def convertTopic(topic: TopicDB): F[Either[ApiError, TopicResponse]]
  def convertImage(image: ImageDB): ImageResponse
  def convertBoard(board: BoardDB): BoardResponse
  def convertReference(reference: ReferenceResponseDB): ReferenceResponse
  def convertImages(images: List[ImageDB]): List[ImageResponse]
  def convertReferences(references: List[ReferenceResponseDB]): List[ReferenceResponse]

}

object ModelConverter {
  def apply[F[_]: Sync](storage: PostgresStorage[F]) = new ModelConverter[F] {
    override def convertPost(post: PostDB): F[Either[ApiError, PostResponse]] = post match {
      case PostDB(id, imageIds, text, createdAt, referencesResponses, referencesFrom, topicId) =>
        (for {
          imagesDb                <- EitherT.right(storage.getImagesBelongToPost(id))
          referencesResponsesFrom <- EitherT(storage.getReferencesBelongToPost(id))
          topic                   <- EitherT(storage.getTopic(topicId))
          referencesResponsesTo   <- EitherT(storage.getReferencesAnswerToPost(id))
        } yield (imagesDb, referencesResponsesFrom, referencesResponsesTo, topic)).map {
          case (
                imagesDb: List[ImageDB],
                referencesResponsesFrom: List[ReferenceResponseDB],
                referencesResponsesTo: List[ReferenceResponseDB],
                topic: TopicDB,
              ) =>
            PostResponse(
              convertImages(imagesDb),
              text,
              createdAt,
              convertReferences(referencesResponsesTo),
              convertReferences(referencesResponsesFrom),
              topic.name,
            )
        }.value
    }

    override def convertTopic(topic: TopicDB): F[Either[ApiError, TopicResponse]] = topic match {
      case TopicDB(id, name, boardId, _) =>
        (for {
          postsDb   <- EitherT.right(storage.getPosts(Some(id)))
          postsHttp <- EitherT(postsDb.map(convertPost).sequence.flatMap(it => Applicative[F].pure(it.sequence)))
          boardDb   <- EitherT(storage.getBoard(boardId))
        } yield (postsHttp, boardDb)).map { case (postsHttp, boardDb: BoardDB) =>
          TopicResponse(name, postsHttp, boardDb.name)
        }.value
    }

    override def convertImage(image: ImageDB): ImageResponse = image match {
      case ImageDB(_, path, _) => ImageResponse(path)
    }

    override def convertImages(images: List[ImageDB]): List[ImageResponse] = images.map(convertImage)

    override def convertReferences(references: List[ReferenceResponseDB]): List[ReferenceResponse] =
      references.map(convertReference)

    override def convertBoard(board: BoardDB): BoardResponse = BoardResponse(board.name)

    override def convertReference(reference: ReferenceResponseDB): ReferenceResponse = reference match {
      case ReferenceResponseDB(_, _, text, postId) => ReferenceResponse(postId, text)
    }
  }
}
