package client;

public class WildcardMethodReferences {
  public static void foo(int i) {}

  public static void bar(int i) {}
  public static void bar(int i, int j) {}

  private native void m2() /*-{
    @WildcardMethodReferences::foo(I)();
    @WildcardMethodReferences::foo(*)();
    @WildcardMethodReferences::bar(II)();
    @WildcardMethodReferences::<error descr="Ambiguous wildcard match: both 'bar(int)' and 'bar(int, int)' in 'client.WildcardMethodReferences' match to 'bar(*)'">bar(*)</error>();
  }-*/;
}