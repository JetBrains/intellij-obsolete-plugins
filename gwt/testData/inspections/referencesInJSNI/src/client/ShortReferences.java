package client;

public class ShortReferences {
  private String field;

  private void method1(int a) {
  }

  private native void method2() /*-{
    this.@ShortReferences::method1(I)();
    @Util::method()();
    @Util.UtilInner::m()();
    @Util.<error descr="Cannot resolve 'Bug'">Bug</error>::m()();
    @ShortReferences::mm()();
    @client.ShortReferences::mm()();
    @Inner::m()();
    @ShortReferences.Inner::m()();
    @client.ShortReferences.Inner::m()();
    @<error descr="Cannot resolve 'NotImported'">NotImported</error>::m()();
    var s = this.@ShortReferences::field;
    s.@java.lang.String::substring(I)(1);
    s.@String::substring(I)(1);
  }-*/;

  public static void mm() {}

  static class Inner {
    public static void m() {}

    private native void nat() /*-{
      @ShortReferences::mm()();
      @client.ShortReferences::mm()();
      @Inner::m()();
      @ShortReferences.Inner::m()();
      @client.ShortReferences.Inner::m()();
    }-*/;
  }
}