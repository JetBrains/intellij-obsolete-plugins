import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Name;

@Name("el_test_2")
public class SeamElAnnotaions_2 {
    @Out("#{completed_bean.description}")
    private String getName(){ return "";}
}