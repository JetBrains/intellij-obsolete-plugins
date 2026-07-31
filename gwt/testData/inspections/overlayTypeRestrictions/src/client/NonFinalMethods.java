package client;

public class NonFinalMethods extends com.google.gwt.core.client.JavaScriptObject {
  protected NonFinalMethods() {
  }

  public static void staticMethod() {}
  private void privateMethod() {}
  public final void finalMethod() {}
  public void instanceMethod() {}
}