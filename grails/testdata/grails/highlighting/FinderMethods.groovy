public class MyDomain {
  int intX;
  int intY;
  String s;
  boolean bool;

  public static void doSomething() {
    MyDomain.findAllByIntX(4);
    MyDomain.findAllByIntXOrIntY(4, 6);
    MyDomain.count();
    MyDomain.listOrderByS();
    MyDomain.listOrderById();
    MyDomain.<warning>findAllByasdfsdfasd</warning>();
    MyDomain.findAllByS<warning>()</warning>;
    MyDomain.findAllByIntX<warning>(3, 4)</warning>;
    MyDomain.findAllByIntX(3);
    MyDomain.findAllByIntXAndS(3, "", [max: 3,
                           offset: 2,
                           sort: "s",
                           order: "desc"]);
  }
}