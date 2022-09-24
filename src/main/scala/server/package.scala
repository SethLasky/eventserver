package object server {
  case class EventCount(impressions: Int, clicks: Int, users: Int){
    override def toString = s"unique_users,$users\nclicks,$clicks\nimpressions,$impressions"
  }

  case class Event(userId: String, event: String, timestamp: Long)
}
