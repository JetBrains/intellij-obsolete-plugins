import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.Component;

import java.util.*;

@Name("authenticator")
class IncorrectCreateDestroyAnnoSignature1 {
    <error>@Create</error> // static method not allowed
    private static void create() {}

    <error>@Destroy</error> // static method not allowed
    private static void destroy() {}
}