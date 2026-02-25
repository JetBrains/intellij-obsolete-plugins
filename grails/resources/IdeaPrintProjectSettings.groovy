/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder

// This constants were copied from org.jetbrains.plugins.grails.config.PrintGrailsSettingsConstants
final String COMPILE = "Compile"
final String RUNTIME = "Runtime"
final String TESTS = "Test"
final String PROVIDED = "Provided"
final String BUILD = "Build"

final String CUSTOM_PLUGIN_PREFIX = "grails.plugin.location."
final String DEBUG_RUN_FORK = "grails.project.fork.run"
final String DEBUG_TEST_FORK = "grails.project.fork.test"

final String SETTINGS_START_MARKER = "---=== IDEA Grails build settings ===---"
final String SETTINGS_END_MARKER = "---=== End IDEA Grails build settings ===---"

// --- === Copied from BuildConfig.groovy === ---
final String WORK_DIR = "grails.work.dir"
final String PROJECT_WORK_DIR = "grails.project.work.dir"
final String PLUGINS_DIR = "grails.project.plugins.dir"
final String GLOBAL_PLUGINS_DIR = "grails.global.plugins.dir"

includeTargets << grailsScript("_GrailsInit")

target('default': "Print project settings") {
  depends(resolveDependencies)
  BuildSettings settings = BuildSettingsHolder.settings

  def forkSettings
  try {
    forkSettings = settings.forkSettings
  }
  catch (Exception ignored) {
    forkSettings = [:]
  }
  Properties properties = new LinkedProperties()

  addProperty(properties, WORK_DIR, settings.getGrailsWorkDir())
  addProperty(properties, PROJECT_WORK_DIR, settings.getProjectWorkDir())
  addProperty(properties, PLUGINS_DIR, settings.getProjectPluginsDir())
  addProperty(properties, GLOBAL_PLUGINS_DIR, settings.getGlobalPluginsDir())
  addProperty(properties, DEBUG_RUN_FORK, forkSettings.run as boolean)
  addProperty(properties, DEBUG_TEST_FORK, forkSettings.test as boolean)

  Map flatten = settings.getConfig().flatten()
  for (Iterator itr = flatten.entrySet().iterator(); itr.hasNext();) {
    Map.Entry entry = (Map.Entry)itr.next()

    Object value = entry.getValue()
    if (value instanceof String || value instanceof GString || value instanceof File) {
      String key = (String)entry.getKey()
      if (key.startsWith(CUSTOM_PLUGIN_PREFIX)) {
        properties.setProperty(key, value.toString())
      }
    }
  }

  storeDependencies(COMPILE, properties, settings.getCompileDependencies())
  storeDependencies(RUNTIME, properties, settings.getRuntimeDependencies())
  storeDependencies(TESTS, properties, settings.getTestDependencies())
  storeDependencies(PROVIDED, properties, settings.getProvidedDependencies())
  storeDependencies(BUILD, properties, settings.getBuildDependencies())

  println()
  println(SETTINGS_START_MARKER)

  properties.store(System.out, "")

  println(SETTINGS_END_MARKER)
}

private void addProperty(Properties properties, String name, Object value) {
  if (value instanceof String || value instanceof GString || value instanceof File || value instanceof Boolean) {
    properties.setProperty(name, value.toString())
  }
}

private void storeDependencies(String env, Properties properties, List dependencies) {
  for (int i = 0; i < dependencies.size(); i++) {
    File file = (File)dependencies.get(i)
    properties.setProperty(env + '.' + i, file.getPath())
  }
}


class LinkedProperties extends Properties {

  private final List list = new ArrayList()

  /**
   * @noinspection UseOfPropertiesAsHashtable
   */
  synchronized Object put(Object key, Object value) {
    list.add(key)
    return super.put(key, value)
  }

  synchronized Enumeration keys() {
    return new ListEnumeration(list, this)
  }
}

class ListEnumeration implements Enumeration {

  private final List list
  private final Properties properties
  private int index = 0

  ListEnumeration(List list, Properties properties) {
    this.list = list
    this.properties = properties
  }

  boolean hasMoreElements() {
    while (index < list.size() && !properties.containsKey(list.get(index))) index++
    return index < list.size()
  }

  Object nextElement() {
    while (index < list.size() && !properties.containsKey(list.get(index))) index++
    return list.get(index++)
  }
}
