class MyDom {
  static def abc() {
    MyDom.createCriteria().get {
      e<caret>q('location', location)
    }
  }
}
