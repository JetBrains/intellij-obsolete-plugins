import org.jboss.seam.annotations.*;
import org.jboss.seam.ScopeType;
import javax.ejb.Stateless;

@Stateless
@Name("st2")
@Scope(ScopeType.STATELESS)
@Roles({
        <error>@Role(name="role_1", scope=ScopeType.APPLICATION)</error>,
        <error>@Role(name="role_2", scope=ScopeType.BUSINESS_PROCESS)</error>,
        <error>@Role(name="role_3", scope=ScopeType.CONVERSATION)</error>,
        <error>@Role(name="role_4", scope=ScopeType.EVENT)</error>,
        <error>@Role(name="role_5", scope=ScopeType.PAGE)</error>,
        @Role(name="role_6", scope=ScopeType.STATELESS),
        <error>@Role(name="role_7", scope=ScopeType.SESSION)</error>,
        @Role(name="role_8", scope=ScopeType.UNSPECIFIED)
    })
class StatelessBean {
}

