import org.jboss.seam.annotations.*;
import org.jboss.seam.ScopeType;
import javax.ejb.Stateful;
import javax.ejb.Remove;

@Stateful
@Name("st1")
<error>@Scope(ScopeType.STATELESS)</error>
@Roles({
        @Role(name="role_1", scope=ScopeType.APPLICATION),
        @Role(name="role_2", scope=ScopeType.BUSINESS_PROCESS),
        @Role(name="role_3", scope=ScopeType.CONVERSATION),
        @Role(name="role_4", scope=ScopeType.EVENT),
        <error>@Role(name="role_5", scope=ScopeType.PAGE)</error>,
        <error>@Role(name="role_6", scope=ScopeType.STATELESS)</error>,
        @Role(name="role_7", scope=ScopeType.SESSION),
        @Role(name="role_8", scope=ScopeType.UNSPECIFIED)
    })
class StatefullBean {
   @Remove
   public void remove(){}
}

