package server.postgre

import doobie._

trait PostgreClient{
  def findOne[X: Read](sql: Fragment) = sql.query[X].option

  def write(sql: Fragment) = sql.update
}
