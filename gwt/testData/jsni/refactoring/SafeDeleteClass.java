public class SafeDeleteClass {
  public static native void method1() /*-{
    @SafeDeleteClass::method2()();
  }-*/;

  public static void method2() {
  }

}