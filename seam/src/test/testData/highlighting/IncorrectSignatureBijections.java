import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Name;

@Name("authenticator")
class IncorrectSignatureBijections {
    @In("blog")
    private void setSetter(Blog a) {}

    <error>@In("blog")</error>
    private void notSetter(Blog a) {}

    <error>@In("blog")</error>
    private void setSetter() {}

    <error>@In("blog")</error>
    private void setSetter(Blog a, Blog b) {}



    <error>@In</error>  // static not allowed
    private static void setSetterStatic(Blog a) {}

    <error>@Out("blog")</error>
    private Blog notGetter(){ return null;}

    <error>@Out("blog")</error>
    private Blog getGetter(int a){ return null;}

    <error>@Out("blog")</error>
    private void gettGetter(){ return;}

    @Out("blog")
    private Blog getGetter(){ return null;}

    <error>@Out("blog")</error>  // static not allowed
    private static Blog getGetterStatic(){ return null;}
}