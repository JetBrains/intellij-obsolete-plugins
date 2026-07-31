class A {
    public static native void method() /*-{
        if (x > 0) {
            console.log("one");
            console.log("two");
        }
    }-*/;
}