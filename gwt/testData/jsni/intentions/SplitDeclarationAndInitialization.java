class A {
    public static native void method() /*-{
        var handler <caret>= function () {
            console.log('1');
        }
    }-*/;
}