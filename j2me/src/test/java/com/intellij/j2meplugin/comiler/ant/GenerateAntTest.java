/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.comiler.ant;

import com.intellij.compiler.ant.BuildTargetsFactory;
import com.intellij.compiler.ant.ModuleChunk;
import com.intellij.j2meplugin.compiler.ant.BuildJarTarget;
import com.intellij.j2meplugin.module.settings.midp.MIDPSettings;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.IdeaTestCase;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.IncorrectOperationException;

import java.io.PrintWriter;
import java.io.StringWriter;

public class GenerateAntTest extends IdeaTestCase {
  public void testP1() throws Exception {
    final VirtualFile parent = getOrCreateModuleDir(myModule);
    PsiTestUtil.setCompilerOutputPath(myModule, parent.getUrl() + "/classes", false);
    checkJarTarget(new ModuleChunk(new Module[]{getModule()}));
  }

  private void checkJarTarget(ModuleChunk chunk) throws Exception {
    final StringWriter targetText = new StringWriter();
    final PrintWriter dataOutput = new PrintWriter(targetText);
    new BuildJarTarget(chunk, BuildTargetsFactory.getInstance().getDefaultOptions(getProject()), new MIDPSettings()).generate(dataOutput);
    dataOutput.flush();
    final String lowercased = StringUtil.toLowerCase(myModule.getName());
    final String expected = "<target name=\"mobile.build.jar." + lowercased + "\"\n" +
                            "        description=\"Build mobile suite for module &apos;" + myModule.getName() + "&apos;\">\n" +
                            "    <property name=\"mobile.path.jar\" value=\"\"/>\n" +
                            "    <jar destfile=\"${mobile.path.jar}\" duplicate=\"preserve\">\n" +
                            "        <zipfileset dir=\"${tmp.dir." + lowercased + "}\"/>\n" +
                            "        <manifest>\n" +
                            "            <attribute name=\"Created-By\" value=\"IntelliJ IDEA\"/>\n" +
                            "            <attribute name=\"Manifest-Version\" value=\"1.0\"/>\n" +
                            "            <attribute name=\"Manifest-Version\" value=\"1.0\"/>\n" +
                            "        </manifest>\n" +
                            "    </jar>\n" +
                            "    <length file=\"${mobile.path.jar}\" property=\"mobile.size.jar\"/>\n" +
                            "    <property name=\"mobile.build.jad.path\" value=\"\"/>\n" +
                            "    <replaceregexp file=\"${mobile.build.jad.path}\" match=\"MIDlet-Jar-Size: .*\"\n" +
                            "                   replace=\"MIDlet-Jar-Size: ${mobile.size.jar}\" byline=\"true\"/>\n" +
                            "</target>";
    checkBuildsEqual(targetText.toString(), expected);
  }

  private void checkBuildsEqual(String generated, String expected) throws IncorrectOperationException {
    final CodeStyleManager manager = CodeStyleManager.getInstance(myProject);
    XmlTag genTag = XmlElementFactory.getInstance(myProject).createTagFromText(StringUtil.convertLineSeparators(generated));
    XmlTag expTag = XmlElementFactory.getInstance(myProject).createTagFromText(StringUtil.convertLineSeparators(expected));
    if (!tagsEqual(genTag, expTag)) {
      genTag = (XmlTag)manager.reformat(manager.reformat(genTag));
      expTag = (XmlTag)manager.reformat(manager.reformat(expTag));
      assertEquals("Text mismatch: ", expTag.getText(), genTag.getText());
    }
  }

  private static boolean tagsEqual(XmlTag genTag, XmlTag expTag) {
    if (!attributesEqual(genTag, expTag)) return false;
    final XmlTag[] gsubTags = genTag.getSubTags();
    final XmlTag[] esubTags = expTag.getSubTags();
    if (gsubTags.length != esubTags.length) return false;
    for (int i = 0; i < esubTags.length; i++) {
      XmlTag esubTag = esubTags[i];
      XmlTag gsubTag = gsubTags[i];
      if (!tagsEqual(gsubTag, esubTag)) return false;
    }
    return true;
  }

  private static boolean attributesEqual(XmlTag genTag, XmlTag expTag) {
    final XmlAttribute[] gattributes = genTag.getAttributes();
    final XmlAttribute[] eattributes = expTag.getAttributes();
    if (gattributes.length != eattributes.length) return false;
    for (int i = 0; i < eattributes.length; i++) {
      XmlAttribute eattribute = eattributes[i];
      XmlAttribute gattribute = gattributes[i];
      // logical comparison of the attributes (namespace:localname and display value)
      if (!Comparing.strEqual(gattribute.getLocalName(), eattribute.getLocalName())) return false;
      if (!Comparing.strEqual(gattribute.getNamespace(), eattribute.getNamespace())) return false;
      if (!Comparing.strEqual(gattribute.getDisplayValue(), eattribute.getDisplayValue())) return false;
    }
    return true;
  }
}