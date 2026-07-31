public class MethodOfWndParameter {
  public native void xxx() /*-{
     $wnd.aler<caret>x
  }-*/;
}