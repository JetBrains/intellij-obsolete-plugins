package com.intellij.frameworks.jboss.seam.rename;

import com.intellij.testFramework.builders.WebModuleFixtureBuilder;

public class SeamRenameContextVariablesTest extends SeamRenameTestCase {

  @Override
  protected void configureModule(WebModuleFixtureBuilder moduleBuilder) throws Exception {
    super.configureModule(moduleBuilder);

    moduleBuilder.addContentRoot(myFixture.getTempDirPath());
    moduleBuilder.addSourceRoot(myFixture.getTempDirPath());
  }

  public void testRenameComponent() {
    myFixture.configureByFiles("Blog.java", "BlogFriend.java");

    myFixture.testRename("component_variable.jsp", "component_variable_after.jsp", "blog_new", "Blog.java", "BlogFriend.java");
    myFixture.checkResultByFile("Blog.java", "Blog_after.java", true);
    myFixture.checkResultByFile("BlogFriend.java", "BlogFriend_after.java", true);
  }

  public void testRenameRole() {
    myFixture.testRename("role_variable1.jsp", "role_variable1_after.jsp", "blog_role_simple_new", "Blog.java");
    myFixture.checkResultByFile("Blog.java", "Blog_after2.java", true);
  }

  public void testRenameRoles() {
    myFixture.configureByFiles("BlogRole.java", "role_variable2.jsp");
    myFixture.testRename("BlogRole_after.java", "blog_role_2_new");
    //myFixture.checkResultByFile("role_variable2.jsp", "role_variable2_after.jsp", true);
  }

  public void testRenameFactory() {
    myFixture.configureByFiles("BlogFactory.java","factory_variable.jsp");
    myFixture.testRename("BlogFactory_after.java", "blog_factory_new");
    //myFixture.checkResultByFile("factory_variable.jsp", "factory_variable_after.jsp", true);
  }
}
