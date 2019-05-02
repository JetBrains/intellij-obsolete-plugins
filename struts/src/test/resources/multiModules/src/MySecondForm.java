import org.apache.struts.validator.ValidatorForm;

public class MySecondForm extends ValidatorForm {

    private String myUsername;
    private String myPassword;

    public String getSecondPassword() {
        return myPassword;
    }

    public void setSecondPassword(String myPassword) {
        this.myPassword = myPassword;
    }

    public String getSecondUsername() {
        return myUsername;
    }

    public void setSecondUsername(String myUsername) {
        this.myUsername = myUsername;
    }
}
