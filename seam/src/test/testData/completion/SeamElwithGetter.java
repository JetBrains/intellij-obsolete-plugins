import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Name;

@Name("el_test_3")
public class SeamElwithGetter {

    private String getName(){ return "#{completed_bean.getD<caret>}";}
}