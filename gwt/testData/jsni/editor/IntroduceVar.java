class A {
    public static native void method() /*-{
        var i = <caret><selection>1</selection>;
    }-*/;
}