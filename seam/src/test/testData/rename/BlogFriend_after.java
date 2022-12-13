import org.jboss.seam.annotations.*;

@Name("blog_friend")
public class BlogFriend {
    @Out("blog_new")
    Blog out;

    @In("#{blog_new.something}")
    String in;

    public String getSomething() {
      return "2 bee or not #{blog_new.something} 2 bee!!!";
    }

    @Begin(id="id=#{blog_new}")
    public void start() {
    }
}