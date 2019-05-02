import org.MyBean;
import org.apache.struts.validator.ValidatorForm;

import java.util.List;

public class MyForm extends ValidatorForm {

    private String myUsername;
    private String myPassword;

    public String getPassword() {
        return myPassword;
    }

    public void setPassword(String myPassword) {
        this.myPassword = myPassword;
    }

    public String getUsername() {
        return myUsername;
    }

    public void setUsername(String myUsername) {
        this.myUsername = myUsername;
    }

    public MyBean[] getBeansArray() {
      return null;
    }

    public List<MyBean> getBeansList() {
      return null;
    }
}
