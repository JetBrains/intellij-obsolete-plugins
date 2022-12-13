import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Name;

@Name("el_test_1")
public class SeamElAnnotaions_1 {
    @Out("#{complete<caret>}")
    private String getName(){ return "";}
}