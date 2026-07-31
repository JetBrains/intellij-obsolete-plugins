public class MethodReferenceForOverloaded {
  public void m(int i){}
  public void m(int i, int j){}

  public native void xxx() /*-{
    this.@MethodReferenceForOverloaded::m<caret>
  }-*/;
}