class Foo {
    private static Foo instance() {
        return new Foo();
    }
    public native Foo doSomething() /*-{
     var foo = @Foo::instance()(); return foo;
   }-*/;
}
