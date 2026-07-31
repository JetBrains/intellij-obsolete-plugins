package pack;

public class JavaClassReference {
  public native void x() /*-{
     @JavaClassRef<caret>
  }-*/;
}