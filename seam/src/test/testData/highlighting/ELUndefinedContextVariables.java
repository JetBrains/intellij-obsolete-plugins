import org.jboss.seam.annotations.*;

@Name("el_test")
class ELUndefinedContextVariables {
    @Out("blog")
    Blog out;

    @In("#{el_test.something}")
    String in;

    @In("#{<warning>unknown</warning>.something}")
    String in2;

    public String getSomething() {
      return "2 bee or not #{blog.<warning>aaa</warning>} 2 bee!!!";
    }
}