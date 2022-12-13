import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.Component;

import java.util.*;

@Name("authenticator3")
class IncorrectCreateDestroyAnnoSignature3 {
    @Create  // single parameter of 'org.jboss.seam.Component' type allowed
    private void create(org.jboss.seam.Component component) {}

    <error>@Destroy</error> // return parameter not allowed
    private String destroy() {return ""; }
}