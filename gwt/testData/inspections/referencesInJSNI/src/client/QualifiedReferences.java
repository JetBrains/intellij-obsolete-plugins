package client;

public class QualifiedReferences {
  private void method1(int a) {
  }

  private native void method2() /*-{
    this.@client.QualifiedReferences::method1(I)();
    @client.Util::method()();
    this.@<error descr="Cannot resolve 'QualifiedReferences'">QualifiedReferences</error>::method1(I)();
    this.@client.QualifiedReferences::<error descr="Cannot resolve symbol 'method1()' in 'client.QualifiedReferences'">method1()</error>();
    this.@<error descr="Cannot resolve 'Klass'">Klass</error>::method1()();
  }-*/;
}