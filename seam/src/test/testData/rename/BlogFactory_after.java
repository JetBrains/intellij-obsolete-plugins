import org.jboss.seam.annotations.*;

@Name("blog")
public class Blog {
    @Factory("blog_factory_new")
    private String createString() { return null;}

    public String getSomething() {
      return null;
    }
}