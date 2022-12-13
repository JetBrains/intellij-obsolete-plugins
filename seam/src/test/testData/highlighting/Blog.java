import org.jboss.seam.annotations.*;

@Name("blog")
@Roles({
        @Role(name="blog_role_1", scope=ScopeType.CONVERSATION),
        @Role(name="blog_role_2", scope=ScopeType.SESSION)
    })
class Blog {
    @DataModel blogs;

    @Factory("blog_factory")
    public String createString() { return null;}

    @Factory("void_type_blog_factory")
    public void createString() { return null;}
}