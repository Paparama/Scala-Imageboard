package ru.dins.scalashool.imageboard.config

import cats.effect.Effect
import pureconfig._
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

object DbConfigLoader {

  def load[F[_]](implicit E: Effect[F]): F[DbConfigModel] =
    ConfigSource.default.at("db").load[DbConfigModel] match {
      case Right(ok) => E.pure(ok)
      case Left(e) => E.raiseError(new ConfigReaderException[DbConfigModel](e))
    }
}