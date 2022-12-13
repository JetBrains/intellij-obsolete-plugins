import org.jboss.seam.annotations.*;
import org.jboss.seam.ScopeType;
import javax.ejb.Stateless;

@Stateless
@Name("st3")
<error>@Scope(ScopeType.APPLICATION)</error>
class StatelessBean2 {
}

