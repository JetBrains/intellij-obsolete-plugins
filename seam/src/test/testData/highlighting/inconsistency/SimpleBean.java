import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.ScopeType;

@Name("simple")
@Roles({
        @Role(name="simple1", scope=ScopeType.CONVERSATION),
        @Role(name="simple2", scope=ScopeType.SESSION)
    })
<error>@JndiName("sss")</error>
class SimpleBean {
}