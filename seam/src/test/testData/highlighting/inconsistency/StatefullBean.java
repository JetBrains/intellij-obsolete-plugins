import org.jboss.seam.annotations.*;
import org.jboss.seam.ScopeType;
import javax.ejb.Stateful;
import javax.ejb.Remove;

@Stateful
@Name("stateful")
<error>@Transactional</error>
class StatefullBean {
   @Remove
   public void remove(){}
}

