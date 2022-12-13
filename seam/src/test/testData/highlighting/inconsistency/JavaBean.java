import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.ScopeType;

//@Name("NoName")
<error>@Roles({
        @Role(name="blog_role_1", scope=ScopeType.CONVERSATION),
        @Role(name="blog_role_2", scope=ScopeType.SESSION)
    })</error>
<error>@Role(name= "blog_role_simple", scope=ScopeType.CONVERSATION)</error>
class JavaBean {
    <error>@DataModel</error> private java.util.List dataModel;

    <error>@Create</error>
    private void create(){}

    <error>@Destroy</error>
    private void destroy() {}
}