class A {
    public static native void method() /*-{
        var handler;
        handler = function () {
            console.log('1');
        };
    }-*/;
}