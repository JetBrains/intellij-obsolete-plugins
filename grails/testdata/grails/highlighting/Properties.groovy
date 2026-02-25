public class MyDomain {
  int intX;
  int intY;
  String s;
  boolean bool;

  public static void doSomething() {
    new MyDomain().save()
    int i=MyDomain.get(0).id+1;
    MyDomain.get(0).<warning descr="Cannot resolve symbol 'fasd'">fasd</warning>();
  }
}