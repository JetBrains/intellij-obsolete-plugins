import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Name;

@Name("type_mismatch")
class BijectionTypeMismatch {
    @Out("void_type_blog_factory")
    private String beep; // IDEA-17544

    @In
    private org.hibernate.search.jpa.FullTextEntityManager entityManager;

    @In
    private void setBlog(Blog blog) {}  // binjected by blog of Blog type

    @Out
    private Blog getBlog(){ return null;}  // binjected by blog of Blog type

    @In("blog")
    private void setBlog2(Blog blog) {}

    @Out("blog2")
    private Blog getBlog2(){ return null;}

    @In("blog_child")
    private void setBlog3(Blog blog) {}

    @Out("blog_child")
    private Blog getBlog3(){ return null;}

    <error>@In("blog")</error>
    private void setBlog4(String blog) {}

    @Out("blog4")
    private String getBlog4(){ return null;}

    @In("blog_role_1")
    private void setBlogRole(Blog blog) {}

    @Out("blog_role_1")
    private Blog getBlogRole(){ return null;}

    @In("blog_factory")
    private void setBlogFactory(String blog) {}

    @Out("blog_factory")
    private String getBlogfactory(){ return "";}

    @In("blog_child")
    private void setBlogChild(Blog blog) {}

    <error>@In("blog")</error>
    private void setBlogChild2(BlogChild blog) {}

}