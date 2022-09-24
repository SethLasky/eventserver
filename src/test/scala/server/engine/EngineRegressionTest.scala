package server.engine

import cats.effect.IO
import cats.implicits._
import fs2.Stream
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Method, Request, Uri}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import server.{Event, EventCount}
import scala.util.Random

class EngineRegressionTest extends AnyWordSpecLike with Matchers with Http4sClientDsl[IO] {
  val port = 8000
  val millisecondsPerHour = 3600000

  def eventsGenerator(number: Int = 20) = {
    val now = System.currentTimeMillis()
    val secondsSinceHour = now % 3600
    val start = now + millisecondsPerHour * secondsSinceHour
    val end = millisecondsPerHour * 2 + start
    def randomLong = Random.between(start, end)
    def randomString = Random.alphanumeric.take(10).mkString

    val timestamps = List(randomLong, randomLong, randomLong)
    val users = List(randomString, randomString, randomString)
    val events = List("click", "impression")
    List.fill(number)(Event(users(Random.nextInt(users.length)), events(Random.nextInt(events.length)), timestamps(Random.nextInt(timestamps.length))))
  }

  def expectedOutputFromEvents(timestamp: Long, originalEvents: List[Event]) = {
    val millisecondsSinceHour = timestamp % millisecondsPerHour
    val start = timestamp - millisecondsSinceHour
    val end = start + millisecondsPerHour
    val events = originalEvents.filter(event => event.timestamp >= start && event.timestamp <= end)
    val users = events.map(_.userId).distinct.size
    val impressions = events.count(_.event == "impression")
    val clicks = events.count(_.event == "click")
    EventCount(impressions, clicks, users).toString
  }

  def runTests(client: Client[IO]) = {
    val events = eventsGenerator()
    val timestamp = events.head.timestamp
    val expectedOutput = expectedOutputFromEvents(timestamp, events)
    events.traverse(writeEventRequest(client)).map(_.forall(_.code == 204) shouldBe true) *>
      getEventsCountRequest(timestamp)(client).map(_ shouldBe expectedOutput)
  }

  def writeEventRequest(client: Client[IO])(event: Event) = {
    val request: Request[IO] = Method.POST(Uri.fromString(s"http://localhost:$port/analytics?timestamp=${event.timestamp}&user_id=${event.userId}&event=${event.event}").right.get)
    client.status(request)
  }

  def getEventsCountRequest(timestamp: Long)(client: Client[IO]) = {
    val request = Method.GET(Uri.fromString(s"http://localhost:$port/analytics?timestamp=$timestamp").right.get)
    client.expect[String](request)
  }

  def checkEngineReadiness(client: Client[IO], port: Int): IO[Unit] = {
    val request = Method.GET(Uri.fromString(s"http://localhost:$port/ping").right.get)
    client.expect[String](request)
  }.flatMap{
    case "pong" => IO.unit
    case _ => checkEngineReadiness(client, port)
  }.handleErrorWith(_ => checkEngineReadiness(client, port))

  "The engine" must {

    "serve two endpoints that write events and display frequency in events by timestamp respectively" in {
      implicit val runtime = cats.effect.unsafe.IORuntime.global
      (for {
        client <- BlazeClientBuilder[IO].stream concurrently Stream.eval(Engine.run(List(port.toString)))
        result <- Stream.eval(checkEngineReadiness(client, port)).evalMap(_ => runTests(client))
      } yield result).compile.drain.unsafeRunSync()
    }
  }

}
