// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.commands;

import com.intellij.codeInsight.TailTypes;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.groovy.mvc.util.MvcTargetDialogCompletionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class GrailsCommandCompletionUtil {
  private static final ExtensionPointName<GrailsCommandProvider> EP_NAME = new ExtensionPointName<>("org.intellij.grails.commandProvider");

  private static final String[] SYSTEM_PROPERTIES = {
    "grails.home",

    // System properties from ivy
    "ivy.default.ivy.user.dir", "ivy.default.conf.dir",
    "ivy.local.default.root", "ivy.local.default.ivy.pattern", "ivy.local.default.artifact.pattern",
    "ivy.shared.default.root", "ivy.shared.default.ivy.pattern", "ivy.shared.default.artifact.pattern",
    "ivy.ivyrep.default.ivy.root", "ivy.ivyrep.default.ivy.pattern", "ivy.ivyrep.default.artifact.root",
    "ivy.ivyrep.default.artifact.pattern",


    // System properties from grails.util.BuildSettings
    "grails.servlet.version", "base.dir", "grails.work.dir", "grails.project.work.dir", "grails.project.war.exploded.dir",
    "grails.project.plugins.dir", "grails.global.plugins.dir", "grails.project.resource.dir", "grails.project.source.dir",
    "grails.project.web.xml", "grails.project.class.dir", "grails.project.plugin.class.dir", "grails.project.plugin.build.class.dir",
    "grails.project.plugin.provided.class.dir", "grails.project.test.class.dir", "grails.project.test.reports.dir",
    "grails.project.docs.output.dir", "grails.project.test.source.dir", "grails.project.target.dir", "grails.project.war.file",
    "grails.project.war.file", "grails.project.war.osgi.headers", "grails.build.listeners", "grails.project.compile.verbose",
    "grails.testing.functional.baseUrl", "grails.compile.artefacts.closures.convert",

    "grails.project.source.level",
    "grails.project.target.level"
  };

  public static final NotNullLazyValue<List<LookupElement>> SYSTEM_PROPERTIES_VARIANTS = NotNullLazyValue.lazy(() -> {
    List<LookupElement> result = new ArrayList<>();
    for (String property : SYSTEM_PROPERTIES) {
      result.add(TailTypeDecorator.withTail(
        LookupElementBuilder.create("-D" + property), MvcTargetDialogCompletionUtils.MyTailTypeEQ.INSTANCE)
      );
    }
    return Collections.unmodifiableList(result);
  });


  public static Collection<LookupElement> collectVariants(@Nullable GrailsApplication application,
                                                          @NotNull String text,
                                                          int offset,
                                                          @NotNull String prefix) {
    if (prefix.startsWith("-D")) {
      return SYSTEM_PROPERTIES_VARIANTS.getValue();
    }
    if (application == null) {
      return Collections.emptyList();
    }
    if (!text.substring(0, offset).matches("\\s*(grails\\s*)?(?:(:?-D\\S+|dev|prod|test)\\s+)*\\S*")) {
      // Command name already typed. Try to complete classes and packages names.
      return MvcTargetDialogCompletionUtils.completeClassesAndPackages(prefix, application.getScope(false, false));
    }

    // complete command name because command name is not typed
    ArrayList<LookupElement> list = new ArrayList<>();
    for (GrailsCommandProvider provider : EP_NAME.getExtensionList()) {
      Collection<String> commands = provider.addCommands(application);
      list.ensureCapacity(commands.size());
      for (String command : commands) {
        list.add(TailTypeDecorator.withTail(LookupElementBuilder.create(command), TailTypes.spaceType()));
      }
    }
    return list;
  }
}
