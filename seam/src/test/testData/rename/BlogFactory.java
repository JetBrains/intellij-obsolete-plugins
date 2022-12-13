import org.jboss.seam.annotations.*;

@Name("blog")
public class Blog {
    @Factory("blog_f<caret>actory")
    private String createString() { return null;}

    public String getSomething() {
      return null;
    }
}