package controllers


import cats.effect.Sync
import cats.implicits._
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.Models.{ApiError, Post, PostCreationBody}

trait TapirAdapter[F[_]] {

  def getPost(id: Long): F[Either[ApiError, Post]]
  def addPost(body: PostCreationBody): F[Either[ApiError, Post]]

}

object TapirAdapter {
  def apply[F[_]: Sync](storage: PostgresStorage[F]) = new TapirAdapter[F] {
    override def getPost(id: Long): F[Either[ApiError, Post]] =
      storage.getPost(id)

    override def addPost(body: PostCreationBody): F[Either[ApiError, Post]] = body match {
      case PostCreationBody(treadId, text, references, imageIds) => storage.createPost(treadId, text, references, imageIds).map(_.asRight)
    }
  }
}
