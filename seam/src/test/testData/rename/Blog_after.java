import org.jboss.seam.annotations.*;

@Name("blog_new")
@Roles({
        @Role(name="blog_role_1", scope=ScopeType.CONVERSATION),
        @Role(name="blog_role_2", scope=ScopeType.SESSION)
    })
@Role(name= "blog_role_simple", scope=ScopeType.CONVERSATION)      
public class Blog {
    @DataModel blogs;

    @Factory("blog_factory")
    private String createString() { return null;}

    public String getSomething() {
      return null;
    }
}