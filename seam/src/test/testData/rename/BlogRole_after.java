import org.jboss.seam.annotations.*;

@Name("bbb")
@Roles({
        @Role(name="blog_role_1", scope=ScopeType.CONVERSATION),
        @Role(name= "blog_role_2_new", scope=ScopeType.SESSION)
    })
@Role(name="blog_role_simple", scope=ScopeType.CONVERSATION)
public class BlogRole {
    public String getSomething() {
      return null;
    }
}