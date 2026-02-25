class Book {
  static belongsTo = [author: Author]
  String name
}

class Author {
  static hasMany = [books: Book]
}

Book.findBy<caret>