import cats.effect.IO
import cats.implicits._
import cats.data.EitherT

def getEith(a: Int): IO[Either[Throwable, List[Int]]] = {
  if (a > 3) IO.pure(Right.apply(List(a,a,a)))
  else IO.pure(Left.apply(new ArithmeticException("AAAA")))
}

def getListIo(a: Int): IO[List[Int]] = IO.pure(List(a,a,a))


def getEithSt(a: String): EitherT[IO, Throwable, String] = {
  if (a.length > 3) EitherT.right(IO.pure("Fine"))
  else EitherT.left(IO.pure((new ArithmeticException("problem"))))
}

def listPlusOneIo(lst: List[Int]): IO[Either[Throwable, List[Int]]] = IO.pure(Right(lst.map(_ + 1)))
def listPlusOne(lst: List[Int]): List[Int] = lst.map(_ + 1)
def plusOne(int: Int): IO[Int] = IO.pure(int + 1)
def plusOneIo(int: Int): IO[Either[Throwable, Int]] = IO.pure(Left(new ArithmeticException("oi")))

def somth = IO.pure(List(1)).flatMap { it =>
  it.map(plusOneIo(_)).sequence.flatMap { listInt => IO.pure(listInt.sequence)
  }
}

def res = (for {
  a <- EitherT.right(getListIo(4))
  c <- EitherT(a.map(plusOneIo).sequence.flatMap { listInt => IO.pure(listInt.sequence)})
  b <- getEithSt("aaaa")
} yield (a, b, c)).map {
  case (a:List[Int], b: String, c: List[Int]) => println(s"$a $b $c")
}

res.value.unsafeRunSync()


