class Domain{
  def foo() {
    Domain.get(0).attach().save().id<caret>
  }
}