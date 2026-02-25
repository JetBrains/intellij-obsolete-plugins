import org.grails.cli.gradle.GradleInvoker

String initScriptPath = System.getenv("INTELLIJ_GRADLE_INIT_SCRIPT")
String[] allArgs = commandLine.remainingArgs
String commandToRun = grails.util.GrailsNameUtils.getNameFromScript(allArgs.head())
String[] commandArguments = allArgs.tail() + commandLine.systemProperties.collect { key, value ->
  "-D${key}=$value".toString()
}

if (initScriptPath) {
  MetaMethod original = GradleInvoker.metaClass.getMetaMethod("invokeMethod", [String.class, Object.class] as Object[])
  GradleInvoker.metaClass.invokeMethod = { String name, Object args ->
    def argsList = args as List<Object>
    argsList.add('--init-script')
    argsList.add(initScriptPath)
    original.invoke(delegate, [name, argsList] as Object[])
  }
}

"$commandToRun"(*commandArguments)
