package ru.dins.scalashool.imageboard.db

object CollectionsNameEnum extends Enumeration {
  val POSTS = Value("posts")
  val BOARDS = Value("boards")
  val TREADS = Value("treads")
  val IMAGES = Value("images")
  val REFERENCES = Value("post_references")
}
