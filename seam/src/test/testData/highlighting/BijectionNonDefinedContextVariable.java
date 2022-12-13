import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.ScopeType;

@Name("context_variable")
class BijectionNonDefinedContextVariable {
    @Out(scope = ScopeType.SESSION)
    private String undefined_name; // IDEA-17549

    @In
    private void setBlog(Blog blog) {}

    @Out
    private Blog getBlog(){ return null;}

    @In("blog")
    private void setBlog2(Blog blog) {}

    @Out("blog")
    private Blog getBlog2(){ return null;}

    @In("blog_role_1")
    private void setBlogRole(Blog blog) {}

    @Out("blog_role_1")
    private Blog getBlogRole(){ return null;}

    @In("blog_factory")
    private void setBlogFactory(String blog) {}

    @Out("blog_factory")
    private String getBlogfactory(){ return "";}

    <warning>@In("undefined_context")</warning>
    private void setUndefined(Blog blog) {}

    @Out("out1")
    private String getOutContextVariable(){ return "";}

    <warning>@In</warning>
    private void setUndefined2(Blog blog) {}

    @Out
    private String getOutContextVariable2(){ return "";}
}