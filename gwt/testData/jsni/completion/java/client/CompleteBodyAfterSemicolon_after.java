package client;

public class CompleteBodyAfterSemicolon {
    public native void xxx() /*-{
        <caret>
    }-*/;
}