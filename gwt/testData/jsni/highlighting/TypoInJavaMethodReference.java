class Bar {
    private static void showString(String s) {}
    public native void doSomething() /*-{
         @Foo::showString(Ljava/lang/String;)("123");
    }-*/;
}
