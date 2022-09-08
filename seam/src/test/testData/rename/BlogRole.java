import org.jboss.seam.annotations.*;

@Name("bbb")
@Roles({
        @Role(name="blog_role_1", scope=ScopeType.CONVERSATION),
        @Role(name="blog_ro<caret>le_2", scope=ScopeType.SESSION)
    })
@Role(name="blog_role_simple", scope=ScopeType.CONVERSATION)
public class BlogRole {
    public String getSomething() {
      return null;
    }
}