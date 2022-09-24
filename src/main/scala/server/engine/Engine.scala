package server.engine

import cats.effect.{ExitCode, IO, IOApp}
import server.config.Config
import server.rest.RestServer
import fs2.Stream
import io.circe.config.parser
import io.circe.generic.auto._

object Engine extends IOApp with RestServer {

  def getConfig(args: List[String]) = parser.decodeF[IO, Config]().map{ config =>
    if(args.nonEmpty) config.copy(http = config.http.copy(port = args.head.toInt)) else config
  }

  def run(args: List[String]): IO[ExitCode] = {
    Stream.eval(getConfig(args)) flatMap server
  }.compile.drain.as(ExitCode.Success)
}
