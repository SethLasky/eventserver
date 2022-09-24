package server.config

import cats.effect.unsafe.IORuntime
import cats.effect.IO
import doobie.util.transactor.Transactor

case class Config(postgre: PostgreConfig, http: HttpConfig)

case class PostgreConfig(url: String, user: String, pass: String){
  def transactor = Transactor.fromDriverManager[IO]("org.postgresql.Driver", url, user, pass)
}

case class HttpConfig(host: String, port: Int)
