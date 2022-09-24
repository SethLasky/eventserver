package server.rest

import org.http4s.{HttpRoutes, Response}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._
import cats.effect.IO
import io.circe.generic.auto._
import server.config.Config
import server.Event
import server.postgre.EventManager
import org.http4s.circe.CirceEntityEncoder._

import scala.concurrent.duration._
import scala.language.postfixOps

trait RestServer extends Http4sDsl[IO] with EventManager {

  object TimestampMatcher extends QueryParamDecoderMatcher[Long]("timestamp")
  object UserMatcher extends QueryParamDecoderMatcher[String]("user_id")
  object EventMatcher extends QueryParamDecoderMatcher[String]("event")

  def server(config: Config) = {
    val transactor = config.postgre.transactor
    val service = HttpRoutes.of[IO] {
      case GET -> Root / "ping" => Ok("pong")
      case GET -> Root / "analytics" :? TimestampMatcher(timestamp) =>
        getEventsCount(timestamp)(transactor).attempt.flatMap(handleResponse)
      case POST -> Root / "analytics" :? TimestampMatcher(timestamp) +& UserMatcher(userId) +& EventMatcher(event) =>
        writeEvent(Event(userId, event, timestamp))(transactor).attempt.flatMap(handleResponse)
    }

    val routes = CORS(service)
    val httpApp = Router("/" -> routes).orNotFound
    BlazeServerBuilder[IO].bindHttp(config.http.port, config.http.host).withHttpApp(httpApp).withIdleTimeout(5.minutes).serve
  }

  private def handleResponse[X](either: Either[Throwable, X]): IO[Response[IO]] = either match {
    case Right(str: String) => Ok(str)
    case Left(err) => BadRequest(s"${err.getMessage}")
    case _ => NoContent()
  }
}
