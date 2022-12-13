import org.jboss.seam.annotations.*;

@Name("blog_friend")
public class BlogFriend {
    @Out("blog")
    Blog out;

    @In("#{blog.something}")
    String in;

    public String getSomething() {
      return "2 bee or not #{blog.something} 2 bee!!!";
    }

    @Begin(id="id=#{blog}")
    public void start() {
    }
}