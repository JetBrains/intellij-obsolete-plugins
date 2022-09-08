import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.Component;

import java.util.*;

@Name("authenticator")
class DublicatedCreateAndDestroyAnnotations {
    <error>@Create</error>
    private void create() {}

    <error>@Destroy</error> 
    private void destroy() {}

    <error>@Create</error>
    private void create2() {}

    <error>@Destroy</error>
    private void destroy2() {}
}