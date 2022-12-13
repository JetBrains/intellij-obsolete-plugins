import org.jboss.seam.annotations.*;
import javax.ejb.Stateless;

@Stateless
@Name("stateless")
<error>@Transactional</error>
class StatelessBean {
    <error>@Create</error>
    private void create(){}

    <error>@Destroy</error>
    private void destroy(){}
}