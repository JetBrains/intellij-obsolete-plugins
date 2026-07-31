class A {
    public static native void method() /*-{
        if (1 < 2) {
            var s = "2";
            s = "3"
        }
    }-*/;
}