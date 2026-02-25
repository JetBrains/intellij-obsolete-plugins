class Ccc {

  static hasMany = [sss:String, n: Nnn]

  String sss;

}

class Fff {
  static belongsTo = [n:Nnn]
}

class Nnn {


  {
    Ccc c = new Ccc();
    System.out << c.sss
    System.out << c.n

    System.out << <warning descr="Cannot resolve symbol 'fff'">fff</warning>

    Fff f = new Fff();
    System.out << f.n

    Nnn n = new Nnn()
  }

}