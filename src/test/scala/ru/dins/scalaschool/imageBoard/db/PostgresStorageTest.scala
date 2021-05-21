package ru.dins.scalaschool.imageBoard.db

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import doobie.Transactor
import doobie.implicits._
import org.flywaydb.core.Flyway
import org.scalamock.scalatest.MockFactory
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.dins.scalashool.imageboard.Storage
import ru.dins.scalashool.imageboard.db.PostgresStorage
import ru.dins.scalashool.imageboard.models.DataBaseModels._
import ru.dins.scalashool.imageboard.models.{ApiError, NotFound, UnprocessableEntity}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class PostgresStorageTest extends AnyFlatSpec with Matchers with TestContainerForAll with MockFactory {
  implicit val cs = IO.contextShift(ExecutionContext.global)

  override val containerDef = PostgreSQLContainer.Def()

  def createTransactor(container: Containers) =
    Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = container.jdbcUrl,
      user = container.username,
      pass = container.password,
    )

  override def startContainers(): PostgreSQLContainer = {
    val container = super.startContainers()
    new Flyway(Flyway.configure().dataSource(container.jdbcUrl, container.username, container.password)).migrate
    container
  }

  def resetStorage(test: (Storage[IO], Transactor.Aux[IO, Unit]) => IO[Assertion]): Unit =
    withContainers { container =>
      val xa = createTransactor(container)
      val truncate =
        sql"truncate post_references CASCADE ; truncate images CASCADE ; truncate posts CASCADE ; truncate topics CASCADE ; truncate boards CASCADE ".update.run
      val storage = PostgresStorage(xa)

      val result = for {
        _         <- truncate.transact(xa)
        assertion <- test(storage, xa)
      } yield assertion

      result.unsafeRunSync()
    }

  def getBoardFromEither(b: Either[ApiError, BoardDB]): BoardDB =
    b.getOrElse(BoardDB(777, "test problems"))

  def getTopicFromEither(t: Either[ApiError, TopicDB]): TopicDB =
    t.getOrElse(TopicDB(777, "test problems", 333, None))

  def getPostFromEither(p: Either[ApiError, PostDB]): PostDB =
    p.getOrElse(PostDB(777, "test problems", LocalDateTime.now(), 444))

  "getBoards" should "return empty list if there is no boards" in resetStorage { case (storage, xa) =>
    storage.getBoards.map(_ shouldBe List())
  }

  it should "return list of BoardDB if there is boards" in resetStorage { case (storage, xa) =>
    for {
      b1     <- storage.createBoard("politic")
      b2     <- storage.createBoard("cars")
      boards <- storage.getBoards
    } yield boards shouldBe List(b1.getOrElse("test problems"), b2.getOrElse("test problems"))
  }

  "getBoard" should "return Right List of BoardWithTopicDB with one element if there is board without topics" in resetStorage {
    case (storage, xa) =>
      for {
        b1    <- storage.createBoard("politic")
        board <- storage.getBoardWithTopic(b1.getOrElse(BoardDB(777, "test problems")).id)
      } yield board shouldBe Right(
        List(BoardWithTopicDB(getBoardFromEither(b1).id, getBoardFromEither(b1).name, None, None)),
      )
  }

  it should "return Right List of BoardWithTopicDB if there is boards and topics" in resetStorage {
    case (storage, xa) =>
      for {
        b1    <- storage.createBoard("politic")
        t1    <- storage.createTopic(b1.getOrElse(BoardDB(777, "test problems")).id, "rff")
        t2    <- storage.createTopic(b1.getOrElse(BoardDB(777, "test problems")).id, "ukk")
        board <- storage.getBoardWithTopic(b1.getOrElse(BoardDB(777, "test problems")).id)
      } yield board shouldBe Right(
        List(
          BoardWithTopicDB(
            getBoardFromEither(b1).id,
            getBoardFromEither(b1).name,
            Some(getTopicFromEither(t1).id),
            Some(getTopicFromEither(t1).name),
          ),
          BoardWithTopicDB(
            getBoardFromEither(b1).id,
            getBoardFromEither(b1).name,
            Some(getTopicFromEither(t2).id),
            Some(getTopicFromEither(t2).name),
          ),
        ),
      )
  }

  it should "return Left NotFound if there is no boards with such ids" in resetStorage { case (storage, xa) =>
    val id = 123
    for {
      board <- storage.getBoardWithTopic(id)
    } yield board shouldBe Left(NotFound(s"Board with id $id not found"))
  }

  it should "return Right List of BoardWithTopicDB sorted by last post at topic creation" in resetStorage {
    case (storage, xa) =>
      for {
        b1    <- storage.createBoard("politic")
        t1    <- storage.createTopic(getBoardFromEither(b1).id, "rff")
        t2    <- storage.createTopic(getBoardFromEither(b1).id, "ukk")
        post  <- storage.createPostTransaction(getTopicFromEither(t2).id, "qwe", List(), List())
        board <- storage.getBoardWithTopic(b1.getOrElse(BoardDB(777, "test problems")).id)
      } yield board shouldBe Right(
        List(
          BoardWithTopicDB(
            getBoardFromEither(b1).id,
            getBoardFromEither(b1).name,
            Some(getTopicFromEither(t2).id),
            Some(getTopicFromEither(t2).name),
          ),
          BoardWithTopicDB(
            getBoardFromEither(b1).id,
            getBoardFromEither(b1).name,
            Some(getTopicFromEither(t1).id),
            Some(getTopicFromEither(t1).name),
          ),
        ),
      )
  }

  "createBoard" should "return Left UnprocessableEntity if there is boards with such name" in resetStorage {
    case (storage, xa) =>
      val name = "pol"
      for {
        _  <- storage.createBoard(name)
        b2 <- storage.createBoard(name)
      } yield b2 shouldBe Left(UnprocessableEntity(s"Name $name already taken"))
  }

  it should "return UnprocessableEntity in name less than 3 chars" in resetStorage { case (storage, xa) =>
    for {
      b1 <- storage.createBoard("as")
    } yield b1 shouldBe Left(UnprocessableEntity(s"Name should contain from 3 to 20 letters"))
  }

  it should "return UnprocessableEntity in name more than 20 chars" in resetStorage { case (storage, xa) =>
    for {
      b1 <- storage.createBoard("a" * 21)
    } yield b1 shouldBe Left(UnprocessableEntity(s"Name should contain from 3 to 20 letters"))
  }

  it should "return Right BoardDB when creation successful, name 3 char length" in resetStorage { case (storage, xa) =>
    val name = "pol"
    for {
      b1 <- storage.createBoard(name)
    } yield b1 shouldBe Right(BoardDB(getBoardFromEither(b1).id, name))
  }

  it should "return Right BoardDB when name is 20 char length" in resetStorage { case (storage, xa) =>
    val name = "a" * 20
    for {
      b1 <- storage.createBoard(name)
    } yield b1 shouldBe Right(BoardDB(getBoardFromEither(b1).id, name))
  }

  "createTopic" should "return Left UnprocessableEntity if there is topic with such name at same board" in resetStorage {
    case (storage, xa) =>
      val boardName = "politic"
      val topicName = "rff"
      for {
        b1 <- storage.createBoard(boardName)
        _  <- storage.createTopic(getBoardFromEither(b1).id, topicName)
        t2 <- storage.createTopic(getBoardFromEither(b1).id, topicName)
      } yield t2 shouldBe Left(UnprocessableEntity(s"Name $topicName already taken"))
  }

  it should "return UnprocessableEntity in name less than 3 chars" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "rf"
    for {
      b1 <- storage.createBoard(boardName)
      er <- storage.createTopic(getBoardFromEither(b1).id, topicName)
    } yield er shouldBe Left(UnprocessableEntity(s"Name should contain from 3 to 20 letters"))
  }

  it should "return UnprocessableEntity in name more than 20 chars" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "a" * 21
    for {
      b1 <- storage.createBoard(boardName)
      er <- storage.createTopic(getBoardFromEither(b1).id, topicName)
    } yield er shouldBe Left(UnprocessableEntity(s"Name should contain from 3 to 20 letters"))
  }

  it should "return UnprocessableEntity if boardId not exist" in resetStorage { case (storage, xa) =>
    val topicName = "rdsfdsf"
    for {
      er <- storage.createTopic(123, topicName)
    } yield er shouldBe Left(UnprocessableEntity(s"Wrong board ID"))
  }

  it should "return Right TopicDB when creation successful, name 3 char length" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "rff"
    for {
      b1 <- storage.createBoard(boardName)
      t1 <- storage.createTopic(getBoardFromEither(b1).id, topicName)
    } yield t1 shouldBe Right(TopicDB(getTopicFromEither(t1).id, topicName, getBoardFromEither(b1).id, None))
  }

  it should "return Right TopicDB when name is 20 char length" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "a" * 20
    for {
      b1 <- storage.createBoard(boardName)
      t1 <- storage.createTopic(getBoardFromEither(b1).id, topicName)
    } yield t1 shouldBe Right(TopicDB(getTopicFromEither(t1).id, topicName, getBoardFromEither(b1).id, None))
  }

  "getTopic" should "return Right List EnrichedTopicDB with one element when there is topic" in resetStorage {
    case (storage, xa) =>
      val boardName = "politic"
      val topicName = "rff"
      for {
        b1   <- storage.createBoard(boardName)
        t1   <- storage.createTopic(getBoardFromEither(b1).id, topicName)
        getT <- storage.getEnrichedTopic(getTopicFromEither(t1).id)
      } yield getT shouldBe Right(
        List(
          EnrichedTopicDB(
            getTopicFromEither(t1).id,
            topicName,
            getBoardFromEither(b1).id,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
            None,
          ),
        ),
      )
  }

  it should "return Right List EnrichedTopicDB with one element when there is topic with post" in resetStorage {
    case (storage, xa) =>
      val boardName = "politic"
      val topicName = "rff"
      for {
        b1   <- storage.createBoard(boardName)
        t1   <- storage.createTopic(getBoardFromEither(b1).id, topicName)
        p1   <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List(), List())
        getT <- storage.getEnrichedTopic(getTopicFromEither(t1).id)
      } yield getT shouldBe Right(
        List(
          EnrichedTopicDB(
            getTopicFromEither(t1).id,
            topicName,
            getBoardFromEither(b1).id,
            Some(getPostFromEither(p1).id),
            Some(getPostFromEither(p1).text),
            Some(getPostFromEither(p1).createdAt),
            None,
            None,
            None,
            None,
            None,
          ),
        ),
      )
  }

  it should "return Right List EnrichedTopicDB when there is topics with posts, ordered by last creation post" in resetStorage {
    case (storage, xa) =>
      val boardName = "politic"
      val topicName = "rff"
      for {
        b1   <- storage.createBoard(boardName)
        t1   <- storage.createTopic(getBoardFromEither(b1).id, topicName)
        p1   <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List(), List())
        p2   <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List(), List())
        getT <- storage.getEnrichedTopic(getTopicFromEither(t1).id)
      } yield getT shouldBe Right(
        List(
          EnrichedTopicDB(
            getTopicFromEither(t1).id,
            topicName,
            getBoardFromEither(b1).id,
            Some(getPostFromEither(p2).id),
            Some(getPostFromEither(p2).text),
            Some(getPostFromEither(p2).createdAt),
            None,
            None,
            None,
            None,
            None,
          ),
          EnrichedTopicDB(
            getTopicFromEither(t1).id,
            topicName,
            getBoardFromEither(b1).id,
            Some(getPostFromEither(p1).id),
            Some(getPostFromEither(p1).text),
            Some(getPostFromEither(p1).createdAt),
            None,
            None,
            None,
            None,
            None,
          ),
        ),
      )
  }

  it should "return Right List EnrichedTopicDB with 2 elements when there is topic with post, post with two references to" in resetStorage {
    case (storage, xa) =>
      val boardName = "politic"
      val topicName = "rff"
      val ref1      = (333.toLong, "asd")
      val ref2      = (444.toLong, "qwe")
      for {
        b1   <- storage.createBoard(boardName)
        t1   <- storage.createTopic(getBoardFromEither(b1).id, topicName)
        p1   <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List(ref1, ref2), List())
        getT <- storage.getEnrichedTopic(getTopicFromEither(t1).id)
      } yield getT shouldBe Right(
        List(
          EnrichedTopicDB(
            getTopicFromEither(t1).id,
            topicName,
            getBoardFromEither(b1).id,
            Some(getPostFromEither(p1).id),
            Some(getPostFromEither(p1).text),
            Some(getPostFromEither(p1).createdAt),
            None,
            None,
            None,
            Some(ref1._2),
            Some(ref1._1),
          ),
          EnrichedTopicDB(
            getTopicFromEither(t1).id,
            topicName,
            getBoardFromEither(b1).id,
            Some(getPostFromEither(p1).id),
            Some(getPostFromEither(p1).text),
            Some(getPostFromEither(p1).createdAt),
            None,
            None,
            None,
            Some(ref2._2),
            Some(ref2._1),
          ),
        ),
      )
  }

  it should "return Right List EnrichedTopicDB with 2 elements when there is topic with post, post with two images" in resetStorage {
    case (storage, xa) =>
      val boardName  = "politic"
      val topicName  = "rff"
      val imagePath1 = "/a/b/c"
      val imagePath2 = "/a/b/d"
      for {
        b1   <- storage.createBoard(boardName)
        t1   <- storage.createTopic(getBoardFromEither(b1).id, topicName)
        p1   <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List(), List(imagePath1, imagePath2))
        getT <- storage.getEnrichedTopic(getTopicFromEither(t1).id)
      } yield getT shouldBe Right(
        List(
          EnrichedTopicDB(
            getTopicFromEither(t1).id,
            topicName,
            getBoardFromEither(b1).id,
            Some(getPostFromEither(p1).id),
            Some(getPostFromEither(p1).text),
            Some(getPostFromEither(p1).createdAt),
            Some(imagePath1),
            None,
            None,
            None,
            None,
          ),
          EnrichedTopicDB(
            getTopicFromEither(t1).id,
            topicName,
            getBoardFromEither(b1).id,
            Some(getPostFromEither(p1).id),
            Some(getPostFromEither(p1).text),
            Some(getPostFromEither(p1).createdAt),
            Some(imagePath2),
            None,
            None,
            None,
            None,
          ),
        ),
      )
  }

  it should "return Right List EnrichedTopicDB with 2 elements when there is topic with post, post with two references from other posts" in resetStorage {
    case (storage, xa) =>
      val boardName = "politic"
      val topicName = "rff"
      val refText1  = "asd"
      val refText2  = "qwe"
      for {
        b1 <- storage.createBoard(boardName)
        t1 <- storage.createTopic(getBoardFromEither(b1).id, topicName)
        t2 <- storage.createTopic(getBoardFromEither(b1).id, "some name")
        p1 <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List(), List())
        p2 <- storage.createPostTransaction(
          getTopicFromEither(t2).id,
          "not qwe",
          List((getPostFromEither(p1).id, refText1), (getPostFromEither(p1).id, refText2)),
          List(),
        )
        getT <- storage.getEnrichedTopic(getTopicFromEither(t1).id)
      } yield getT shouldBe Right(
        List(
          EnrichedTopicDB(
            getTopicFromEither(t1).id,
            topicName,
            getBoardFromEither(b1).id,
            Some(getPostFromEither(p1).id),
            Some(getPostFromEither(p1).text),
            Some(getPostFromEither(p1).createdAt),
            None,
            Some(refText2),
            Some(getPostFromEither(p2).id),
            None,
            None,
          ),
          EnrichedTopicDB(
            getTopicFromEither(t1).id,
            topicName,
            getBoardFromEither(b1).id,
            Some(getPostFromEither(p1).id),
            Some(getPostFromEither(p1).text),
            Some(getPostFromEither(p1).createdAt),
            None,
            Some(refText1),
            Some(getPostFromEither(p2).id),
            None,
            None,
          ),
        ),
      )
  }

  "createPostTransaction" should "return Left UnprocessableEntity if there is no topic with such id" in resetStorage {
    case (storage, xa) =>
      for {
        er <- storage.createPostTransaction(1123, "qwe", List(), List())
      } yield er shouldBe Left(UnprocessableEntity(s"Wrong topic ID"))
  }

  "createSubscribe" should "return Left UnprocessableEntity if there is no topic with such id" in resetStorage {
    case (storage, xa) =>
      for {
        er <- storage.createSubscribe("asd@mail.ru", 213)
      } yield er shouldBe Left(UnprocessableEntity(s"Wrong topic ID"))
  }

  it should "return Left UnprocessableEntity if subscribe on such topic by this email already exist" in resetStorage {
    case (storage, xa) =>
      val boardName = "politic"
      val topicName = "rff"
      val email     = "asd@mail.ru"
      for {
        b1 <- storage.createBoard(boardName)
        t1 <- storage.createTopic(getBoardFromEither(b1).id, topicName)
        s1 <- storage.createSubscribe(email, getTopicFromEither(t1).id)
        er <- storage.createSubscribe(email, getTopicFromEither(t1).id)
      } yield er shouldBe Left(UnprocessableEntity(s"$email already get notifications"))
  }

  it should "return Unit if subscribe created" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "rff"
    val email     = "asd@mail.ru"
    for {
      b1 <- storage.createBoard(boardName)
      t1 <- storage.createTopic(getBoardFromEither(b1).id, topicName)
      s1 <- storage.createSubscribe(email, getTopicFromEither(t1).id)
    } yield s1 shouldBe Right(())
  }

  "deleteSubscription" should "return 1 if it delete 1 row" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "rff"
    val email     = "asd@mail.ru"
    for {
      b1  <- storage.createBoard(boardName)
      t1  <- storage.createTopic(getBoardFromEither(b1).id, topicName)
      s1  <- storage.createSubscribe(email, getTopicFromEither(t1).id)
      del <- storage.deleteSubscription(email, getTopicFromEither(t1).id)
    } yield del shouldBe 1
  }

  "getSubscribers" should "return List of emails if there subscribers on topic" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "rff"
    val email1    = "asd@mail.ru"
    val email2    = "asd2@mail.ru"
    for {
      b1     <- storage.createBoard(boardName)
      t1     <- storage.createTopic(getBoardFromEither(b1).id, topicName)
      t2     <- storage.createTopic(getBoardFromEither(b1).id, "asd")
      s1     <- storage.createSubscribe(email1, getTopicFromEither(t1).id)
      s2     <- storage.createSubscribe(email2, getTopicFromEither(t1).id)
      s3     <- storage.createSubscribe(email1, getTopicFromEither(t2).id)
      emails <- storage.getSubscribers(getTopicFromEither(t1).id)
    } yield emails shouldBe List(email1, email2)
  }

  "getSubscribers" should "return empty List of emails if there no subscribers on topic" in resetStorage {
    case (storage, xa) =>
      val boardName = "politic"
      val topicName = "rf"
      val email1    = "asd@mail.ru"
      val email2    = "asd2@mail.ru"
      for {
        b1     <- storage.createBoard(boardName)
        t1     <- storage.createTopic(getBoardFromEither(b1).id, topicName)
        t2     <- storage.createTopic(getBoardFromEither(b1).id, "asd")
        s1     <- storage.createSubscribe(email1, getTopicFromEither(t1).id)
        s2     <- storage.createSubscribe(email2, getTopicFromEither(t1).id)
        emails <- storage.getSubscribers(getTopicFromEither(t2).id)
      } yield emails shouldBe List()
  }

  "getSubscribers" should "return empty List of emails if there no topic" in resetStorage { case (storage, xa) =>
    for {
      emails <- storage.getSubscribers(123)
    } yield emails shouldBe List()
  }

  it should "return 0 if it nothing delete" in resetStorage { case (storage, xa) =>
    val email = "asd@mail.ru"
    for {
      del <- storage.deleteSubscription(email, 123)
    } yield del shouldBe 0
  }

  "deleteTopic" should "return Unit if it nothing delete" in resetStorage { case (storage, xa) =>
    for {
      del <- storage.deleteTopic(123)
    } yield del shouldBe ()
  }

  it should "return Unit if it delete topic" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "rff"
    for {
      b1  <- storage.createBoard(boardName)
      t1  <- storage.createTopic(getBoardFromEither(b1).id, topicName)
      del <- storage.deleteTopic(getTopicFromEither(t1).id)
    } yield del shouldBe ()
  }

  "getCountPostsById" should "return 0 if no such post" in resetStorage { case (storage, xa) =>
    for {
      res <- storage.getCountPostsById(List(1, 2, 3))
    } yield res shouldBe 0
  }

  it should "return 2 if 2 of 3 post are found" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "rff"
    for {
      b1  <- storage.createBoard(boardName)
      t1  <- storage.createTopic(getBoardFromEither(b1).id, topicName)
      p1  <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List(), List())
      p2  <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List(), List())
      res <- storage.getCountPostsById(List(getPostFromEither(p1).id, getPostFromEither(p2).id, 100))
    } yield res shouldBe 2
  }

  "createPostTransaction" should "return UnprocessableEntity in reference more than 1000 chars" in resetStorage {
    case (storage, xa) =>
      val boardName = "politic"
      val topicName = "rff"
      val refText   = "a" * 1001
      for {
        b1 <- storage.createBoard(boardName)
        t1 <- storage.createTopic(getBoardFromEither(b1).id, topicName)
        er <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List((1, refText)), List())
      } yield er shouldBe Left(UnprocessableEntity("reference should contain from 3 to 1000 letters"))
  }

  it should "return UnprocessableEntity in reference less than 3 chars" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "rff"
    val refText   = "aa"
    for {
      b1 <- storage.createBoard(boardName)
      t1 <- storage.createTopic(getBoardFromEither(b1).id, topicName)
      er <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List((1, refText)), List())
    } yield er shouldBe Left(UnprocessableEntity("reference should contain from 3 to 1000 letters"))
  }

  it should "return Right PostDB model if reference text length is 999" in resetStorage { case (storage, xa) =>
    val boardName = "politic"
    val topicName = "rff"
    val refText   = "a" * 999
    val postText   = "qwe"
    for {
      b1 <- storage.createBoard(boardName)
      t1 <- storage.createTopic(getBoardFromEither(b1).id, topicName)
      p1 <- storage.createPostTransaction(getTopicFromEither(t1).id, "qwe", List((1, refText)), List())
    } yield p1 shouldBe Right(PostDB(getPostFromEither(p1).id, postText, getPostFromEither(p1).createdAt, getTopicFromEither(t1).id))
  }
}
