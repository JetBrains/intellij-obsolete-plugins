package client;

public class MyClass {
  private void mmm(int a) {
  }

  private native void method2() /*-{
    this.@client.MyClass::mmm(I)<error descr="Incorrect number of arguments for 'MyClass.mmm(int)' method: 1 expected but 0 found">()</error>;
    @client.MyClass::<error descr="Cannot call instance method 'MyClass.mmm()' without object instance">mmm(I)</error>(1);
    this.@client.MyClass::mmm(I)(1);
    @client.MyClass::new()();
  }-*/;

  public MyClass() {
  }
}