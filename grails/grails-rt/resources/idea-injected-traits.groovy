import grails.compiler.traits.TraitInjector
import org.grails.core.io.support.GrailsFactoriesLoader

def MARKER = '--------------------------------------'

def injectors = GrailsFactoriesLoader.loadFactories(
  TraitInjector.class, Thread.currentThread().contextClassLoader
)

try {
  injectors = org.grails.compiler.injection.TraitInjectionSupport.resolveTraitInjectors(injectors)
}
catch (Throwable ignored) {
}

println()
println()
println()
println MARKER
injectors.each {
  Class traitClass = it.trait
  println traitClass.name
  println it.artefactTypes.length
  it.artefactTypes.each {
    println it
  }
}
println MARKER
