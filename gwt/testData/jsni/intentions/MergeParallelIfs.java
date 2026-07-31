class A {
    public static native void method() /*-{
        if (<caret>x > 0) {
            console.log("one");
        }
        if (x > 0) {
            console.log("two");
        }
    }-*/;
}