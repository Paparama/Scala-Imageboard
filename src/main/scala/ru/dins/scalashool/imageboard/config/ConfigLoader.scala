package ru.dins.scalashool.imageboard.config

import cats.effect.Effect
import pureconfig._
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class DbConfigModel (driver: String, url: String, user: String, password: String)
case class MailConfigModel (senderEmail: String, host: String, port: Int, password: String)
case class AppConfigModel (port: Int, topicLimit: Int, uploadDir: String)
case class AllConfigModel (db: DbConfigModel, mail: MailConfigModel, app: AppConfigModel)

object ConfigLoader {

  def load[F[_]](implicit E: Effect[F]): F[AllConfigModel] =
    ConfigSource.default.load[AllConfigModel] match {
      case Right(ok) => E.delay(ok)
      case Left(e) => E.raiseError(new ConfigReaderException[AllConfigModel](e))
    }
}