package ru.dins.scalashool.imageboard.mailClient

import cats.effect._
import cats.implicits._

import javax.mail._
import javax.mail.Message.RecipientType.{CC, TO}
import javax.mail.internet._
import ru.dins.scalashool.imageboard.config.MailConfigModel

sealed trait MailClient[F[_]] {
  def send(to: List[String], content: String): F[Unit]
}

object MailClient {

  case class SafeMimeMessage(
                              from:    InternetAddress,
                              to:      List[InternetAddress],
                              cc:      List[InternetAddress],
                              subject: String,
                              content: String
                            ) {
    def toAddressString: String = to.map(_.toString).intercalate(", ")
    def ccAddressString: String = cc.map(_.toString).intercalate(", ")
    def toMimeMessage(session: Session): MimeMessage = {
      val m = new MimeMessage(session)
      m.setFrom(from)
      to.toList.foreach(m.addRecipient(TO, _))
      cc.foreach(m.addRecipient(CC, _))
      m.setSubject(subject)
      m.setContent(content, "text/plain")
      m
    }
  }

  def apply[F[_]: Sync](config: MailConfigModel): MailClient[F] =
    new MailClient[F] {

      val session = {
        val props = new java.util.Properties
        props.put("mail.smtp.auth", true)
        props.put("mail.smtp.password", config.password)
        props.put("mail.smtp.host", config.host)
        props.put("mail.smtp.port", config.port.toString)
        props.put("mail.smtp.socketFactory.port", config.port.toString)
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
        props.put("mail.smtp.socketFactory.fallback", "false")
        props.setProperty("mail.smtp.ssl.trust", config.host)
        Session.getInstance(props)
      }

      override def send(to: List[String], content: String): F[Unit] = {
        val form = config.senderEmail
        val subject = "New message at topic you subscribe"
        to match {
          case Nil => Sync[F].delay(())
          case _ =>
            val m = SafeMimeMessage(new InternetAddress(form), to.map(new InternetAddress(_)), List(), subject, content)
            Sync[F].delay(Transport.send(m.toMimeMessage(session), config.senderEmail, config.password))
        }

      }
    }

}