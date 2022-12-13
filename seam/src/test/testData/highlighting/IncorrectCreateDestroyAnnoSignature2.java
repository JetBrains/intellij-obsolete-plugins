import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.Component;

import java.util.*;

@Name("authenticator2")
class IncorrectCreateDestroyAnnoSignature2 {
    <error>@Create</error> // parameters not allowed
    private void create(int aaa) {}

    <error>@Destroy</error>   // parameters not allowed
    private void destroy(int bbb) {}
}