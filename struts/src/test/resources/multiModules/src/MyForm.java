import org.apache.struts.validator.ValidatorForm;

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
}
