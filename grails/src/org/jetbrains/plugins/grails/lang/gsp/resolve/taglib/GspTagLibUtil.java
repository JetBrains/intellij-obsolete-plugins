// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.resolve.taglib;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.search.AllClassesSearchExecutor;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.PairProcessor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GspTagLibUtil {

  private static final Set<String> NOT_TAGS =
    ContainerUtil.newHashSet("grailsApplication", "out", "grailsUrlMappingsHolder", "typeConverter", "renderNoSelectionOption");

  public static final @NonNls String DEFAULT_TAGLIB_PREFIX = "g";
  public static final @NonNls String DYNAMIC_TAGLIB_PACKAGE = "org.codehaus.groovy.grails.plugins.web.taglib";
  public static final @NonNls String NAMESPACE_FIELD = "namespace";

  private GspTagLibUtil() {
  }

  public static Set<String> getExcludedTags(String prefix) {
    return DEFAULT_TAGLIB_PREFIX.equals(prefix) ? NOT_TAGS : Collections.emptySet();
  }

  public static TagLibNamespaceDescriptor getTagLibClasses(@NotNull PsiElement place, @NotNull String tagLibPrefix) {
    return getTagLibClasses(place).get(tagLibPrefix);
  }

  public static Map<String, TagLibNamespaceDescriptor> getTagLibClasses(final @NotNull PsiElement place) {
    Module module = ModuleUtilCore.findModuleForPsiElement(place);
    if (module == null) return Collections.emptyMap();

    return getTagLibClasses(module);
  }

  public static Map<String, TagLibNamespaceDescriptor> getTagLibClasses(final @NotNull Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(
      module,
      () -> CachedValueProvider.Result.create(
        computeCustomTaglibClasses(module),
        PsiModificationTracker.MODIFICATION_COUNT
      )
    );
  }

  private static Map<String, TagLibNamespaceDescriptor> computeCustomTaglibClasses(Module module) {
    final Map<String, TagLibNamespaceDescriptor> res = new HashMap<>();

    res.put(DEFAULT_TAGLIB_PREFIX, new TagLibNamespaceDescriptor(DEFAULT_TAGLIB_PREFIX, module)); // Maps key set must  contain default prefix!!!

    final Collection<GrClassDefinition> taglibs = new LinkedHashSet<>();
    taglibs.addAll(GrailsArtifact.TAGLIB.getInstances(module).values());
    taglibs.addAll(searchTaglibsInClassPath(module));

    for (GrClassDefinition classDefinition : taglibs) {
      String prefix = getPrefixByTagLibClass(classDefinition);

      TagLibNamespaceDescriptor descr = res.get(prefix);
      if (descr == null) {
        descr = new TagLibNamespaceDescriptor(prefix, module);
        res.put(prefix, descr);
      }

      descr.addClass(classDefinition);
    }

    PsiPackage psiPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage(DYNAMIC_TAGLIB_PACKAGE);
    if (psiPackage != null) {
      for (PsiClass tagLibClass : psiPackage.getClasses(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))) {
        String name = tagLibClass.getName();
        if (name != null && name.endsWith(GrailsArtifact.TAGLIB.suffix)) {
          String prefix = TAGLIB_PREFEXES.get(tagLibClass.getQualifiedName());
          if (prefix == null) {
            prefix = DEFAULT_TAGLIB_PREFIX;
          }

          TagLibNamespaceDescriptor descr = res.get(prefix);
          if (descr == null) {
            descr = new TagLibNamespaceDescriptor(prefix, module);
            res.put(prefix, descr);
          }

          descr.addClass(tagLibClass);
        }
      }
    }

    return res;
  }

  private static @NotNull Collection<GrClassDefinition> searchTaglibsInClassPath(final @NotNull Module module) {
    final Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(module, () -> CachedValueProvider.Result.create(
      doSearchTaglibsInClassPath(module),
      PsiModificationTracker.MODIFICATION_COUNT,
      MvcModuleStructureSynchronizer.getInstance(project).getFileAndRootsModificationTracker()
    ));
  }

  private static @NotNull Collection<GrClassDefinition> doSearchTaglibsInClassPath(final @NotNull Module module) {
    final Project project = module.getProject();
    final GlobalSearchScope scope = module.getModuleRuntimeScope(false);

    final List<String> taglibNames = new ArrayList<>();
    AllClassesSearchExecutor.processClassNames(project, scope, fqn -> {
      if (fqn.endsWith(GrailsArtifact.TAGLIB.suffix)) {
        taglibNames.add(fqn);
      }
      return true;
    });

    final PsiShortNamesCache namesCache = PsiShortNamesCache.getInstance(project);
    final Collection<GrClassDefinition> taglibs = new LinkedHashSet<>();
    for (String name : taglibNames) {
      final PsiClass[] classes = namesCache.getClassesByName(name, scope);
      for (PsiClass clazz : classes) {
        if (!(clazz instanceof PsiCompiledElement)) continue;
        final PsiElement mirror = clazz.getNavigationElement();
        if (!(mirror instanceof GrClassDefinition)) continue;
        taglibs.add((GrClassDefinition)mirror);
      }
    }
    return taglibs;
  }

  private static final Map<String, String> TAG_2_FQN_1_2;
  private static final Map<String, String> TAG_2_FQN_3;
  private static final Map<String, String> FQN_2_TAG;

  static {
    Map<String, String> tag2Fqn12 = new HashMap<>(11);
    // List of these tags get from org.codehaus.groovy.grails.web.taglib.GrailsTagRegistry.instance. Check this list when grails releases. #CHECK#
    tag2Fqn12.put("renderInput", "org.codehaus.groovy.grails.web.taglib.RenderInputTag");
    tag2Fqn12.put("each", "org.codehaus.groovy.grails.web.taglib.GroovyEachTag");
    tag2Fqn12.put("if", "org.codehaus.groovy.grails.web.taglib.GroovyIfTag");
    tag2Fqn12.put("unless", "org.codehaus.groovy.grails.web.taglib.GroovyUnlessTag");
    tag2Fqn12.put("else", "org.codehaus.groovy.grails.web.taglib.GroovyElseTag");
    tag2Fqn12.put("elseif", "org.codehaus.groovy.grails.web.taglib.GroovyElseIfTag");
    tag2Fqn12.put("findAll", "org.codehaus.groovy.grails.web.taglib.GroovyFindAllTag");
    tag2Fqn12.put("collect", "org.codehaus.groovy.grails.web.taglib.GroovyCollectTag");
    tag2Fqn12.put("grep", "org.codehaus.groovy.grails.web.taglib.GroovyGrepTag");
    tag2Fqn12.put("while", "org.codehaus.groovy.grails.web.taglib.GroovyWhileTag");
    tag2Fqn12.put("def", "org.codehaus.groovy.grails.web.taglib.GroovyDefTag");

    Map<String, String> tag2Fqn3 = new HashMap<>(10);
    tag2Fqn3.put("each", "org.grails.gsp.compiler.tags.GroovyEachTag");
    tag2Fqn3.put("if", "org.grails.gsp.compiler.tags.GroovyIfTag");
    tag2Fqn3.put("unless", "org.grails.gsp.compiler.tags.GroovyUnlessTag");
    tag2Fqn3.put("else", "org.grails.gsp.compiler.tags.GroovyElseTag");
    tag2Fqn3.put("elseif", "org.grails.gsp.compiler.tags.GroovyElseIfTag");
    tag2Fqn3.put("findAll", "org.grails.gsp.compiler.tags.GroovyFindAllTag");
    tag2Fqn3.put("collect", "org.grails.gsp.compiler.tags.GroovyCollectTag");
    tag2Fqn3.put("grep", "org.grails.gsp.compiler.tags.GroovyGrepTag");
    tag2Fqn3.put("while", "org.grails.gsp.compiler.tags.GroovyWhileTag");
    tag2Fqn3.put("def", "org.grails.gsp.compiler.tags.GroovyDefTag");

    Map<String, String> fqn2Tag = new HashMap<>(tag2Fqn12.size() + tag2Fqn3.size());
    fqn2Tag.putAll(ContainerUtil.reverseMap(tag2Fqn12));
    fqn2Tag.putAll(ContainerUtil.reverseMap(tag2Fqn3));

    TAG_2_FQN_1_2 = tag2Fqn12;
    TAG_2_FQN_3 = tag2Fqn3;
    FQN_2_TAG = fqn2Tag;
  }

  /**
   * This is list of standard TagLib classes which have namespace different from 'g'
   * Standard TagLib classes are a TagLib classes from package 'org.codehaus.groovy.grails.plugins.web.taglib' (see computeCustomTaglibClasses() method)
   * Check this list when grails releases. #CHECK#
   */
  public static final Map<String, String> TAGLIB_PREFEXES = new HashMap<>();
  static {
    TAGLIB_PREFEXES.put("org.codehaus.groovy.grails.plugins.web.taglib.PluginTagLib", "plugin");
    TAGLIB_PREFEXES.put("org.codehaus.groovy.grails.plugins.web.taglib.SitemeshTagLib", "sitemesh");
  }

  public static boolean isSdkTagLib(@NotNull PsiClass aClass) {
    String name = aClass.getQualifiedName();
    return FQN_2_TAG.containsKey(name) ||
           DYNAMIC_TAGLIB_PACKAGE.equals(ClassUtil.extractPackageName(name));
  }

  public static @Nullable String getTagNameByClass(@NotNull String className) {
    return FQN_2_TAG.get(className);
  }

  public static @NotNull String getPrefixByTagLibClass(GrClassDefinition clazz) {
    PsiField field = clazz.findCodeFieldByName(NAMESPACE_FIELD, true);
    GrField grField = null;
    if (field instanceof GrField) {
      grField = (GrField)field;
    }
    else if (field != null) {
      PsiElement source = field.getNavigationElement();
      if (source instanceof GrField) {
        grField = (GrField)source;
      }
    }
    if (grField == null) return DEFAULT_TAGLIB_PREFIX;
    if (grField.hasModifierProperty(PsiModifier.STATIC)) {
      GrExpression initializer = grField.getInitializerGroovy();
      if (initializer != null) {
        final Object value = JavaPsiFacade.getInstance(clazz.getProject()).getConstantEvaluationHelper().computeConstantExpression(initializer);
        if (value instanceof String) {
          String res = ((String)value).trim();
          return res.isEmpty() ? DEFAULT_TAGLIB_PREFIX : res;
        }
      }
    }
    return DEFAULT_TAGLIB_PREFIX;
  }

  private static @NotNull Map<String, String> getTag2Fqn(@NotNull PsiElement place) {
    GrailsApplication application = GrailsApplicationManager.findApplication(place);
    return application == null || application.getGrailsVersion().isLessThan(Version.GRAILS_3_0) ? TAG_2_FQN_1_2 : TAG_2_FQN_3;
  }

  public static void processBuiltInTagClasses(@Nullable String tagName,
                                              @NotNull PsiElement place,
                                              @NotNull PairProcessor<? super String, ? super PsiClass> processor) {
    Map<String, String> tag2Fqn = getTag2Fqn(place);
    if (tagName == null) {
      processBuiltInTagClasses(tag2Fqn, place, processor);
    }
    else {
      PsiClass builtInClass = getBuiltInTagByName(tag2Fqn, tagName, place);
      if (builtInClass == null) {
        return;
      }
      processor.process(tagName, builtInClass);
    }
  }

  private static @Nullable PsiClass getBuiltInTagByName(@NotNull Map<String, String> tag2Fqn,
                                                        @NotNull String tagName,
                                                        @NotNull PsiElement place) {
    String fqn = tag2Fqn.get(tagName);
    if (fqn == null) {
      return null;
    }
    final PsiClass builtInClass = JavaPsiFacade.getInstance(place.getProject()).findClass(fqn, place.getResolveScope());
    if (builtInClass != null) {
      return builtInClass;
    }
    return null;
  }

  private static void processBuiltInTagClasses(@NotNull Map<String, String> tag2Fqn,
                                               @NotNull PsiElement place,
                                               @NotNull PairProcessor<? super String, ? super PsiClass> processor) {
    final JavaPsiFacade facade = JavaPsiFacade.getInstance(place.getProject());
    final GlobalSearchScope resolveScope = place.getResolveScope();
    for (Map.Entry<String, String> entry : tag2Fqn.entrySet()) {
      final PsiClass builtInClass = facade.findClass(entry.getValue(), resolveScope);
      if (builtInClass != null && !processor.process(entry.getKey(), builtInClass)) {
        return;
      }
    }
  }

  public static boolean processGrailsTags(PsiScopeProcessor processor,
                                          PsiElement ref,
                                          ResolveState state,
                                          @Nullable String name,
                                          @Nullable ElementClassHint classHint) {
    return processGrailsTags(processor, ref, state, name, classHint, getTagLibClasses(ref));
  }

  public static boolean processGrailsTags(PsiScopeProcessor processor,
                                          PsiElement ref,
                                          ResolveState state,
                                          @Nullable String name,
                                          @Nullable ElementClassHint classHint,
                                          Map<String, TagLibNamespaceDescriptor> tagClassMap) {

    if (ResolveUtil.shouldProcessProperties(classHint)) {
      // process tags prefixes
      if (name == null) {
        for (TagLibNamespaceDescriptor descriptor : tagClassMap.values()) {
          PsiVariable variable = descriptor.getDummyClassVariable();
          if (variable != null && !processor.execute(variable, ResolveState.initial())) return false;
        }
      }
      else {
        if (ref.getParent() instanceof GrReferenceExpression) {
          TagLibNamespaceDescriptor descriptor = tagClassMap.get(name);
          if (descriptor != null) {
            PsiVariable variable = descriptor.getDummyClassVariable();
            if (variable != null && !processor.execute(variable, ResolveState.initial())) return false;
          }
        }
      }
    }

    if (ResolveUtil.shouldProcessMethods(classHint)) {
      TagLibNamespaceDescriptor descriptor = tagClassMap.get(DEFAULT_TAGLIB_PREFIX);
      if (descriptor != null) {
        if (!descriptor.processTags(processor, state, name)) return false;
      }
    }

    return true;
  }

  public static Pair<Map<String, XmlAttributeDescriptor>, Set<String>> getAttributesDescriptorsFromJavadocs(PsiElement place) {
    PsiElement element = place.getNavigationElement().getNavigationElement();
    return CachedValuesManager.getCachedValue(element, () -> {
      Map<String, XmlAttributeDescriptor> javaDocAttrMap = null;

      if (element instanceof PsiDocCommentOwner) {
        PsiDocComment docComment = ((PsiDocCommentOwner)element).getDocComment();
        if (docComment != null) {
          PsiDocTag[] attrs = docComment.findTagsByName("attr");
          if (attrs.length > 0) {
            javaDocAttrMap = new HashMap<>();

            for (final PsiDocTag attr : attrs) {
              XmlAttributeDescriptor descriptor = createDescriptorByDoc(attr);
              if (descriptor != null) {
                javaDocAttrMap.put(descriptor.getName(), descriptor);
              }
            }
          }
        }
      }

      if (javaDocAttrMap == null) {
        javaDocAttrMap = Collections.emptyMap();
      }

      Set<String> sourceAttrName = null;

      if (element instanceof GrField) {
        for (String attrName : ((GrField)element).getNamedParameters().keySet()) {
          if (javaDocAttrMap.containsKey(attrName)) continue;
          if ("tagName".equals(attrName) || "type".equals(attrName) || "remove".equals(attrName)) continue;

          if (sourceAttrName == null) {
            sourceAttrName = new HashSet<>();
          }
          sourceAttrName.add(attrName);
        }
      }

      if (sourceAttrName == null) {
        sourceAttrName = Collections.emptySet();
      }

      return CachedValueProvider.Result.create(Pair.create(javaDocAttrMap, sourceAttrName), element);
    });
  }

  private static @Nullable XmlAttributeDescriptor createDescriptorByDoc(final PsiDocTag docTag) {
    final PsiDocTagValue valueElement = docTag.getValueElement();
    if (valueElement == null) return null;

    String name = valueElement.getText().trim();
    if (name.isEmpty()) return null;

    PsiElement[] dataElements = docTag.getDataElements();
    final boolean isRequired = dataElements.length > 1
                               && StringUtil.startsWithIgnoreCase(dataElements[1].getText().trim(), "required");

    return new AnyXmlAttributeDescriptor(name) {
      @Override
      public boolean isRequired() {
        return isRequired;
      }

      @Override
      public PsiElement getDeclaration() {
        return valueElement;
      }
    };
  }

}
