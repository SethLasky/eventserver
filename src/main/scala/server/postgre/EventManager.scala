package server.postgre

import cats.effect.IO
import doobie.util.transactor.Transactor.Aux
import doobie.implicits._
import server.{Event, EventCount}

trait EventManager extends PostgreClient {

  private def createCountQuery(timestamp: Long) = {
    val millisecondsPerHour = 3600000
    val millisecondsSinceHour = timestamp % millisecondsPerHour
    val start = timestamp - millisecondsSinceHour
    val end = start + millisecondsPerHour
    sql"SELECT COUNT(*) FILTER (WHERE event = 'impression') AS impressions, COUNT(*) FILTER (WHERE event = 'click') AS clicks, COUNT(DISTINCT(userId)) AS users FROM events  WHERE timestamp BETWEEN $start AND $end"
  }

  def getEventsCount(timestamp: Long)(transactor: Aux[IO, Unit]) = {
    findOne[EventCount](createCountQuery(timestamp)).transact(transactor).map(_.map(_.toString).getOrElse(s"No events for timestamp $timestamp"))
  }

  def writeEvent(event: Event)(transactor: Aux[IO, Unit]) =
    write(sql"INSERT INTO events (userid, event, timestamp) VALUES(${event.userId}, ${event.event}, ${event.timestamp})").run.transact(transactor)
}
