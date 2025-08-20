package com.intellij.play.utils;

import com.intellij.openapi.project.Project;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

import java.util.*;

public final class PlayClassExtensions {
  private static final List<PlayClassExtension> myExtensions = new ArrayList<>();
  private static final PlayClassExtension myStringArrayExtension = createStringArrayExtensions();


  static {
    myExtensions.add(createStringExtensions());
    myExtensions.add(createObjectExtensions());
    myExtensions.add(createNumberExtensions());
    myExtensions.add(createCollectionExtensions());
    myExtensions.add(createDateExtensions());
    myExtensions.add(createLongExtensions());
    myExtensions.add(createMapExtensions());
  }

  private static PlayClassExtension createStringArrayExtensions() {
    final PlayClassExtension extension = new PlayClassExtension("StringArray") {

      @Override
      protected boolean acceptPsiClass(@NotNull PsiClass psiClass) {
        if(psiClass instanceof PsiArrayType) {
          return CommonClassNames.JAVA_LANG_STRING.equals(((PsiArrayType)psiClass).getComponentType().getCanonicalText());
        }
        return false;
      }
    };

    extension.addExtension("add", "java.lang.String[]");
    extension.addExtension("contains", CommonClassNames.JAVA_LANG_BOOLEAN, CommonClassNames.JAVA_LANG_STRING);
    extension.addExtension("remove", "java.lang.String[]", CommonClassNames.JAVA_LANG_STRING);

    return extension;
  }

  private static PlayClassExtension createMapExtensions() {
    final PlayClassExtension extension = new PlayClassExtension(CommonClassNames.JAVA_UTIL_MAP);

    extension.addExtension("asAttr", "play.templates.Template.ExecutableTemplate.RawData");
    extension.addExtension("asAttr", "play.templates.Template.ExecutableTemplate.RawData", CommonClassNames.JAVA_LANG_STRING);

    return extension;
  }

  private static PlayClassExtension createLongExtensions() {
    final PlayClassExtension extension = new PlayClassExtension(CommonClassNames.JAVA_LANG_LONG);

    extension.addExtension("asdate", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING);
    extension
      .addExtension("asdate", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING);
    extension.addExtension("formatSize");

    return extension;
  }

  private static PlayClassExtension createDateExtensions() {
    final PlayClassExtension extension = new PlayClassExtension(CommonClassNames.JAVA_UTIL_DATE);

    extension.addExtension("format", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING);
    extension
      .addExtension("format", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING);
    extension.addExtension("since");
    extension.addExtension("since", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_BOOLEAN);

    return extension;
  }

  private static PlayClassExtension createCollectionExtensions() {
    final PlayClassExtension extension = new PlayClassExtension(CommonClassNames.JAVA_UTIL_COLLECTION);

    extension.addExtension("join", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING);
    extension.addExtension("last", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_OBJECT);
    extension.addExtension("pluralize");
    extension.addExtension("pluralize", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING);
    extension.addExtension("pluralize", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING,
                           CommonClassNames.JAVA_LANG_STRING);

    return extension;
  }

  private static PlayClassExtension createNumberExtensions() {
    final PlayClassExtension extension = new PlayClassExtension(CommonClassNames.JAVA_LANG_OBJECT);

    extension.addExtension("divisibleBy", CommonClassNames.JAVA_LANG_NUMBER, CommonClassNames.JAVA_LANG_BOOLEAN);
    extension.addExtension("format", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING);
    extension.addExtension("formatCurrency", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING);
    extension.addExtension("page", CommonClassNames.JAVA_LANG_INTEGER, CommonClassNames.JAVA_LANG_NUMBER);
    extension.addExtension("pluralize");

    return extension;
  }

  private static PlayClassExtension createObjectExtensions() {
    final PlayClassExtension extension = new PlayClassExtension(CommonClassNames.JAVA_LANG_OBJECT);

    extension.addExtension("addSlashes");
    extension.addExtension("capAll");
    extension.addExtension("capFirst");
    extension.addExtension("cut", CommonClassNames.JAVA_LANG_STRING, CommonClassNames.JAVA_LANG_STRING);
    extension.addExtension("escape");
    extension.addExtension("nl2br");
    extension.addExtension("raw", "play.templates.Template.ExecutableTemplate.RawData");
    extension.addExtension("yesNo");

    return extension;
  }

  private static PlayClassExtension createStringExtensions() {
    final PlayClassExtension extension = new PlayClassExtension(CommonClassNames.JAVA_LANG_STRING);

    extension.addExtension("asXml", "groovy.util.slurpersupport.GPathResult");
    extension.addExtension("camelCase");
    extension.addExtension("capitalizeWords");
    extension.addExtension("escapeHtml");
    extension.addExtension("escapeJavaScript");
    extension.addExtension("escapeXml");
    extension.addExtension("noAccents");
    extension.addExtension("pad");
    extension.addExtension("slugify");
    extension.addExtension("urlEncode");

    return extension;
  }

  public static List<GrMethod> getStringArrayExtensions(@NotNull Project project) {
    return mapExtensions(project, myStringArrayExtension.getExtensions()) ;
  }

  public static List<GrMethod> getExtensions(@NotNull final PsiClass psiClass) {
    List<GrMethod> exts = new ArrayList<>();

    for (PlayClassExtension extension : myExtensions) {

        exts.addAll(mapExtensions(psiClass.getProject(), extension.getExtensions(psiClass)));
      }

    return exts;
  }

  private static List<GrMethod> mapExtensions(final @NotNull Project project, List<PlayMethodExtension> extensions) {
    return ContainerUtil.mapNotNull(extensions, methodExtension -> {
      final GrLightMethodBuilder builder = new GrLightMethodBuilder(PsiManager.getInstance(project), methodExtension.getName());

      builder.setReturnType(methodExtension.getReturnType(), GlobalSearchScope.allScope(project));

      int i = 0;
      for (String paramType : methodExtension.getParamTypes()) {
        builder.addParameter(("arg" + i++), paramType);
      }
      return builder;
    });
  }

  private static class PlayClassExtension {
      private final String myClassFqn;
      protected List<PlayMethodExtension> myExtensions = new ArrayList<>();

      private PlayClassExtension(@NotNull String classFqn) {
        myClassFqn = classFqn;
      }

      public void addExtension(String extension) {
        myExtensions.add(new PlayMethodExtension(extension));
      }

      public void addExtension(String name, String returnType, String... paramTypes) {
        myExtensions.add(new PlayMethodExtension(name, returnType, paramTypes));
      }

      public List<PlayMethodExtension> getExtensions() {
         return myExtensions;
      }

      public List<PlayMethodExtension> getExtensions(@NotNull PsiClass psiClass) {
        return acceptPsiClass(psiClass) ? myExtensions : Collections.emptyList();
      }

      protected boolean acceptPsiClass(PsiClass psiClass) {
        return InheritanceUtil.isInheritor(psiClass, myClassFqn);
      }
    }

    private static final class PlayMethodExtension {
      private final String myName;
      private final String myReturnType;
      private final Set<String> myParamTypes = new LinkedHashSet<>();

      private PlayMethodExtension(@NotNull String name) {
        this(name, CommonClassNames.JAVA_LANG_STRING);
      }

      private PlayMethodExtension(String name, String returnType, String... paramTypes) {
        myName = name;
        myReturnType = returnType;
        Collections.addAll(myParamTypes, paramTypes);
      }

      public String getName() {
        return myName;
      }

      public String getReturnType() {
        return myReturnType;
      }

      public Set<String> getParamTypes() {
        return myParamTypes;
      }
    }

}
