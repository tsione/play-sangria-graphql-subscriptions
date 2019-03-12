package models

/**
  * A case class used for publishing events related to the Post entity.
  *
  * @param name the name of the event to be published
  * @param post an instance of Post to be published
  */
case class PostEvent(name: String, post: Post) extends Event
