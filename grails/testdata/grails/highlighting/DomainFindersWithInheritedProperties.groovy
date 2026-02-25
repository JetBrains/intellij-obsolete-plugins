class Domain1 {
  def x;
  def getAbc(){return 2;}
}

class Domain2 extends Domain1 {
  def y;

  def foo() {
    Domain2 d = Domain2.findByXAndY(1, 2)
    Domain2 d2 = Domain2.findByAbcAndY(1, 2)
    print d;
    print d2;
  }
}

