import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.ScopeType;

@Name("illegal_scope_test")
class BijectionIllegalScopeDeclaration {
    @In(value="blog", create=true)
    private void setBlog1(Blog blog) {}

    @In(value="blog",create=true, scope=ScopeType.UNSPECIFIED)
    private void setBlog2(Blog blog) {}

    <error>@In(value="blog",create=true, scope=ScopeType.APPLICATION)</error>
    private void setBlog3(Blog blog) {}

    <error>@In(value="blog", create=true, scope=ScopeType.BUSINESS_PROCESS)</error>
    private void setBlog4(Blog blog) {}

   <error>@In(value="blog", create=true, scope=ScopeType.CONVERSATION)</error>
    private void setBlog5(Blog blog) {}

   <error>@In(value="blog", create=true, scope=ScopeType.EVENT)</error>
    private void setBlog6(Blog blog) {}

   <error>@In(value="blog", create=true, scope=ScopeType.PAGE)</error>
    private void setBlog7(Blog blog) {}

   <error>@In(value="blog", create=true, scope=ScopeType.STATELESS)</error>
    private void setBlog8(Blog blog) {}

    <error>@In(value="blog", create=true, scope=ScopeType.SESSION)</error>
    private void setBlog9(Blog blog) {}

    @Out(value="blog", scope = ScopeType.UNSPECIFIED)
    private Blog  b_1;
    <error>@Out(value="blog", scope = ScopeType.APPLICATION)</error>
    private Blog  b_2;
    <error>@Out(value="blog", scope = ScopeType.BUSINESS_PROCESS)</error>
    private Blog  b_3;
    <error>@Out(value="blog", scope = ScopeType.CONVERSATION)</error>
    private Blog  b_4;
    <error>@Out(value="blog", scope = ScopeType.EVENT)</error>
    private Blog  b_5;
    <error>@Out(value="blog", scope = ScopeType.PAGE)</error>
    private Blog  b_6;
    <error>@Out(value="blog", scope = ScopeType.STATELESS)</error>
    private Blog  b_7;
    <error>@Out(value="blog", scope = ScopeType.SESSION)</error>
    private Blog  b_8;

    @Out(value="blog_unknown1", scope = ScopeType.UNSPECIFIED)
    private Blog  b_11;
    @Out(value="blog_unknown2", scope = ScopeType.APPLICATION)
    private Blog  b_12;
    @Out(value="blog_unknown3", scope = ScopeType.BUSINESS_PROCESS)
    private Blog  b_13;
    @Out(value="blog_unknown4", scope = ScopeType.CONVERSATION)
    private Blog  b_14;
    @Out(value="blog_unknown5", scope = ScopeType.EVENT)
    private Blog  b_15;
    @Out(value="blog_unknown6", scope = ScopeType.PAGE)
    private Blog  b_16;
    @Out(value="blog_unknown7", scope = ScopeType.STATELESS)
    private Blog  b_17;
    @Out(value="blog_unknown8", scope = ScopeType.SESSION)
    private Blog  b_18;
}