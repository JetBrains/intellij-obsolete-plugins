try {
  import org.apache.commons.lang.exception.ExceptionUtils
  import org.apache.spark.sql.SparkSession

  import java.io.{PrintWriter, StringWriter}
  import java.util
  import scala.collection.mutable.ListBuffer
  import scala.collection.{immutable, mutable}
  import scala.reflect.api.JavaUniverse
  import scala.tools.nsc.interpreter.IMain
  import org.json4s.jackson.Serialization
  import org.json4s.{Formats, NoTypeHints}

  import java.util.function.{Function => JFunction}
  import java.util.regex.Pattern
  import scala.language.implicitConversions
  import scala.util.Try
  import org.apache.spark.sql.Dataset
  import org.apache.spark.rdd.RDD
  import org.apache.spark.SparkContext

  trait Loopback {
    def pass(obj: Any, id: String): Any
  }

  object ResNames {
    val REF = "ref"
    val VALUE = "value"
    val IS_PRIMITIVE = "isPrimitive"
    val TYPE = "type"
    val TIME = "time"
    val LENGTH = "length"
    val LAZY = "lazy"
  }

  object TrieMap {
    class Node[T](var value: Option[T]) {
      var children: mutable.Map[String, TrieMap.Node[T]] = _

      def put(key: String, node: TrieMap.Node[T]): Option[Node[T]] = {
        if (children == null)
          children = mutable.Map[String, TrieMap.Node[T]]()
        children.put(key, node)
      }

      def del(key: String): Option[Node[T]] = children.remove(key)

      def forEach(func: Function[T, _]): Unit = {
        func.apply(value.get)
        if (children != null) children.foreach(t => t._2.forEach(func))
      }
    }

    def split(key: String): Array[String] = {
      var n = 0
      var j = 0
      for (i <- 0 until key.length) {
        if (key.charAt(i) == '.') n += 1
      }
      val k = new Array[String](n + 1)
      val sb = new mutable.StringBuilder(k.length)
      for (i <- 0 until key.length) {
        val ch = key.charAt(i)
        if (ch == '.') {
          k({
            j += 1;
            j - 1
          }) = sb.toString
          sb.setLength(0)
        }
        else sb.append(ch)
      }
      k(j) = sb.toString
      k
    }
  }

  class TrieMap[T] {
    val root = new TrieMap.Node[T](null)

    def subtree(key: Array[String], length: Int): TrieMap.Node[T] = {
      var current = root
      var i = 0
      while ( {
        i < length && current != null
      }) {
        if (current.children == null) return null
        current = current.children.get(key(i)).orNull
        i += 1
      }
      current
    }

    def put(key: Array[String], value: T): Option[TrieMap.Node[T]] = {
      val node = subtree(key, key.length - 1)
      node.put(key(key.length - 1), new TrieMap.Node[T](Option.apply(value)))
    }

    def put(key: String, value: T): Option[TrieMap.Node[T]] = {
      val k = TrieMap.split(key)
      put(k, value)
    }

    def contains(key: String): Boolean = {
      val k = TrieMap.split(key)
      val node = subtree(k, k.length)
      node != null
    }

    def get(key: String): Option[T] = {
      val k = TrieMap.split(key)
      val node = subtree(k, k.length)
      if (node == null) return Option.empty
      node.value
    }

    def subtree(key: String): TrieMap.Node[T] = {
      val k = TrieMap.split(key)
      subtree(k, k.length)
    }
  }

  trait TypeHandler {
    def accept(obj: Any): Boolean

    def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any]

    def getErrors: List[String] = List[String]()
  }

  abstract class AbstractCollectionHandler(limit: Int, timeout: Int) extends AbstractTypeHandler {
    trait Iterator {
      def hasNext: Boolean

      def next: Any
    }

    def iterator(obj: Any): Iterator

    def length(obj: Any): Int

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] = {
      if (depth <= 0) {
        withJsonObject { result =>
          var s = scalaInfo.value.toString
          if (s.length>1000)
            s = s.take(1000) + "..."
          result += (ResNames.VALUE -> s)
          return result
        }
      } else {
        mutable.Map[String, Any](
          ResNames.LENGTH -> length(scalaInfo.value),
          ResNames.VALUE -> withJsonArray { json =>
            val startTime = System.currentTimeMillis()
            val it = iterator(scalaInfo.value)
            var index = 0
            while (it.hasNext && index < limit && !checkTimeoutError(scalaInfo.path, startTime, timeout)) {
                val id = scalaInfo.path
                json += loopback.pass(it.next, s"$id[$index]")
                index += 1
            }
            })
      }
    }
  }

  abstract class AbstractTypeHandler extends TypeHandler {
    val timeoutErrors: mutable.MutableList[String] = mutable.MutableList()

    override def getErrors: List[String] = timeoutErrors.toList

    protected def withJsonArray(body: mutable.MutableList[Any] => Unit): mutable.MutableList[Any] = {
      val arr = mutable.MutableList[Any]()
      body(arr)
      arr
    }

    protected def withJsonObject(body: mutable.Map[String, Any] => Unit): mutable.Map[String, Any] = {
      val obj = mutable.Map[String, Any]()
      body(obj)
      obj
    }

    protected def wrap(obj: Any, tpe: String): mutable.Map[String, Any] = mutable.Map[String, Any](
      ResNames.VALUE -> Option(obj).orNull,
      ResNames.TYPE -> tpe
    )

    protected def checkTimeoutError(name: String, startTime: Long, timeout: Int): Boolean = {
      val isTimeout = System.currentTimeMillis() - startTime > timeout
      if (isTimeout)
        timeoutErrors += f"Variable $name collect timeout exceed ${timeout}ms."
      isTimeout
    }

  }

  class ArrayHandler(limit: Int, timeout: Int) extends AbstractCollectionHandler(limit, timeout) {
    override def accept(obj: Any): Boolean = obj.isInstanceOf[Array[_]]

    override def length(obj: Any): Int = obj.asInstanceOf[Array[_]].length

    override def iterator(obj: Any): Iterator = new Iterator {
      private val it = obj.asInstanceOf[Array[_]].iterator

      override def hasNext: Boolean = it.hasNext

      override def next: Any = it.next
    }
  }

  class JavaCollectionHandler(limit: Int, timeout: Int) extends AbstractCollectionHandler(limit, timeout) {
    override def accept(obj: Any): Boolean = obj.isInstanceOf[util.Collection[_]]

    override def iterator(obj: Any): Iterator = new Iterator() {
      private val it = obj.asInstanceOf[util.Collection[_]].iterator()

      override def hasNext: Boolean = it.hasNext

      override def next: Any = it.next()
    }

    override def length(obj: Any): Int = obj.asInstanceOf[util.Collection[_]].size()
  }
  class MapHandler(limit: Int, timeout: Int) extends AbstractTypeHandler {
    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] =
      withJsonObject {
        json =>
          val obj = scalaInfo.value
          val id = scalaInfo.path
          val map = obj.asInstanceOf[Map[_, _]]
          val keys = mutable.MutableList[Any]()
          val values = mutable.MutableList[Any]()
          json += ("jvm-type" -> obj.getClass.getCanonicalName)
          json += ("length" -> map.size)
          var index = 0

          json += ("key" -> keys)
          json += ("value" -> values)

          val startTime = System.currentTimeMillis()
          map.view.take(math.min(limit, map.size)).foreach {
            case (key, value) =>
              if (checkTimeoutError(scalaInfo.path, startTime, timeout))
                return json
              keys += loopback.pass(key, s"$id.key[$index]")
              values += loopback.pass(value, s"$id.value[$index]")
              index += 1
          }
      }

    override def accept(obj: Any): Boolean = obj.isInstanceOf[Map[_, _]]
  }

  class NullHandler extends AbstractTypeHandler {
    override def accept(obj: Any): Boolean = obj == null

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] =
      mutable.Map[String, Any]()
  }

  class ObjectHandler(val stringSizeLimit: Int,
                      val manager: HandlerManager,
                      val referenceManager: ReferenceManager,
                      val timeout: Int) extends AbstractTypeHandler {
    private val INACCESSIBLE = ScalaVariableInfo(isAccessible = false, isLazy = false, null, null, null, null)
    val ru: JavaUniverse = scala.reflect.runtime.universe
    val mirror: ru.Mirror = ru.runtimeMirror(getClass.getClassLoader)
    import scala.reflect.runtime.universe.NoSymbol
    case class ReflectionProblem(e: Throwable, symbol: String, var count: Int)

    val problems: mutable.Map[String, ReflectionProblem] = mutable.Map[String, ReflectionProblem]()

    override def accept(obj: Any): Boolean = true

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] =
      withJsonObject { result =>
        val obj = scalaInfo.value

        if (obj == null) {
          return result
        }
        if (depth <= 0) {
          var s = obj.toString
          if (s.length>stringSizeLimit)
            s = s.take(stringSizeLimit) + "..."
          result += (ResNames.VALUE -> s)
          return result
        }

        val startTime = System.currentTimeMillis()
        val fields = listAccessibleProperties(scalaInfo, startTime)
        if (fields.isEmpty) {
          var s = obj.toString
          if (s.length>stringSizeLimit)
            s = s.take(stringSizeLimit) + "..."
          result += (ResNames.VALUE -> s)
          return result
        }

        val resolvedFields = mutable.Map[String, Any]()
        result += (ResNames.VALUE -> resolvedFields)


        fields.foreach { field =>
          if (checkTimeoutError(field.name, startTime, timeout)) {
            return result
          }

          if (field.ref != null && field.ref != field.path) {
            resolvedFields += (field.name -> (mutable.Map[String, Any]() += (ResNames.REF -> field.ref)))
          } else {
            resolvedFields += (field.name -> manager.handleVariable(field, loopback, depth - 1))
          }
        }

        result
      }


    override def getErrors: List[String] = problems.map(x =>
      f"Reflection error for ${x._2.symbol} counted ${x._2.count}.\n" +
        f"Error message: ${ExceptionUtils.getMessage(x._2.e)}\n " +
        f"Stacktrace:${ExceptionUtils.getStackTrace(x._2.e)}").toList ++ super.getErrors

    private def listAccessibleProperties(info: ScalaVariableInfo, startTime: Long): List[ScalaVariableInfo] = {
      val instanceMirror = mirror.reflect(info.value)
      val instanceSymbol = instanceMirror.symbol
      val members = instanceSymbol.toType.members

      val parsedMembers = mutable.MutableList[ScalaVariableInfo]()
      members.foreach { symbol =>
        if (checkTimeoutError(info.path, startTime, timeout))
          return parsedMembers.toList
        val variableInfo = get(instanceMirror, symbol, info.path)
        if (variableInfo.isAccessible)
          parsedMembers += variableInfo
      }

      parsedMembers.toList
    }

    private def get(instanceMirror: ru.InstanceMirror, symbol: ru.Symbol, path: String): ScalaVariableInfo = {
      if (!problems.contains(path))
        try {
          // is public property
          if (!symbol.isMethod && symbol.isTerm && (symbol.asTerm.isVar || symbol.asTerm.isVal)
          && symbol.asTerm.getter != NoSymbol
          && symbol.asTerm.getter.isPublic) {
            val term = symbol.asTerm
            val f = instanceMirror.reflectField(term)
            val fieldPath = s"$path.${term.name.toString.trim}"
            val value = f.get
            val tpe = term.typeSignature.toString
            return ScalaVariableInfo(isAccessible = tpe != "<notype>", isLazy = term.isLazy, value, tpe,
              fieldPath, referenceManager.getRef(value, fieldPath))
          }
        } catch {
          case e: Throwable => problems(path) = ReflectionProblem(e, symbol.toString, 1)
        }
      else
        problems(path).count += 1

      INACCESSIBLE
    }
  }

  class PrimitiveHandler extends AbstractTypeHandler {
    override def accept(obj: Any): Boolean =
      obj match {
        case _: Byte => true
        case _: Short => true
        case _: Boolean => true
        case _: Char => true
        case _: Int => true
        case _: Long => true
        case _: Float => true
        case _: Double => true
        case _ => false
      }

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] =
      mutable.Map[String, Any](
        ResNames.VALUE -> scalaInfo.value,
        ResNames.IS_PRIMITIVE -> 1
      )
  }

  class SeqHandler(limit: Int, timeout: Int) extends AbstractCollectionHandler(limit, timeout) {
    override def accept(obj: Any): Boolean = obj.isInstanceOf[Seq[_]]

    override def iterator(obj: Any): Iterator = new Iterator {
      private val it = obj.asInstanceOf[Seq[_]].iterator

      override def hasNext: Boolean = it.hasNext

      override def next: Any = it.next()
    }

    override def length(obj: Any): Int = obj.asInstanceOf[Seq[_]].size
  }

  class SetHandler(limit: Int, timeout: Int) extends AbstractCollectionHandler(limit, timeout) {
    override def iterator(obj: Any): Iterator = new Iterator {
      private val it = obj.asInstanceOf[Set[_]].iterator

      override def hasNext: Boolean = it.hasNext

      override def next: Any = it.next()
    }

    override def length(obj: Any): Int = obj.asInstanceOf[Set[_]].size

    override def accept(obj: Any): Boolean = obj.isInstanceOf[Set[_]]
  }

  class SpecialsHandler(limit: Int) extends AbstractTypeHandler {
    override def accept(obj: Any): Boolean = obj.getClass.getCanonicalName != null && obj.getClass.getCanonicalName.startsWith("scala.")

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] = withJsonObject {
      json =>
        var s = scalaInfo.value.toString
        if (s.length>limit)
          s = s.take(limit) + "..."
        json.put(ResNames.VALUE, s)
    }
  }

  class StringHandler(limit: Int) extends AbstractTypeHandler {
    override def accept(obj: Any): Boolean = obj.isInstanceOf[String]

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] = {
      var s = scalaInfo.value.asInstanceOf[String]
      if (s.length>limit)
        s = s.take(limit) + "..."
      mutable.Map(
        ResNames.VALUE -> s
      )
    }
  }

  class ThrowableHandler extends AbstractTypeHandler {
    override def accept(obj: Any): Boolean = obj.isInstanceOf[Throwable]

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] = {
      val obj = scalaInfo.value
      val throwable = obj.asInstanceOf[Throwable]
      val writer = new StringWriter()
      val out = new PrintWriter(writer)
      throwable.printStackTrace(out)

      mutable.Map(
        ResNames.VALUE -> writer.toString
      )
    }
  }

  class HandlerManager(enableProfiling: Boolean,
                       timeout: Int,
                       stringSizeLimit: Int,
                       collectionSizeLimit: Int,
                       referenceManager: ReferenceManager) {
    private val handlerChain = ListBuffer[AbstractTypeHandler](
      new NullHandler(),
      new StringHandler(stringSizeLimit),
      new ArrayHandler(collectionSizeLimit, timeout),
      new JavaCollectionHandler(collectionSizeLimit, timeout),
      new SeqHandler(collectionSizeLimit, timeout),
      new SetHandler(collectionSizeLimit, timeout),
      new MapHandler(collectionSizeLimit, timeout),
      new ThrowableHandler(),
      new SpecialsHandler(stringSizeLimit),
      new PrimitiveHandler(),
      new DatasetHandler(),
      new RDDHandler(),
      new SparkContextHandler(),
      new SparkSessionHandler(),
      new ObjectHandler(stringSizeLimit, this, referenceManager, timeout)
    ).map(new HandlerWrapper(_, enableProfiling))

    def getErrors: mutable.Seq[String] = handlerChain.flatMap(x => x.handler.getErrors)

    def handleVariable(info: ScalaVariableInfo, loopback: Loopback, depth: Int, startTime: Long = System.currentTimeMillis()): Any = {
      handlerChain.find(_.accept(info)).map(_.handle(info, loopback, depth, startTime)).getOrElse(mutable.Map[String, Any]())
    }
  }

  class HandlerWrapper(val handler: TypeHandler, profile: Boolean) {
    def accept(info: ScalaVariableInfo): Boolean = info.isLazy || handler.accept(info.value)

    def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int, initStartTime: Long): Any = {
      val startTime = if (initStartTime != null)
        initStartTime
      else
        System.currentTimeMillis()

      val data = if (scalaInfo.isLazy) {
        mutable.Map[String, Any](ResNames.LAZY -> true)
      }
      else {
        try {
          val data = handler.handle(scalaInfo, loopback, depth: Int)
          if (data.keys.count(_ == ResNames.IS_PRIMITIVE) > 0) {
            return data(ResNames.VALUE)
          }
          data
        } catch {
          case t: Throwable =>
            return ExceptionUtils.getRootCauseMessage(t)
        }

      }
      try {
        data.put(ResNames.TYPE, calculateType(scalaInfo))
      } catch {
        case t: Throwable =>
          data.put(ResNames.TYPE, ExceptionUtils.getRootCauseMessage(t))
      }

      if (profile)
        data.put(ResNames.TIME, System.currentTimeMillis() - startTime)

      data
    }

    private def calculateType(scalaInfo: ScalaVariableInfo): String = {
      if (scalaInfo.tpe != null)
        return scalaInfo.tpe

      if (scalaInfo.value != null)
        scalaInfo.value.getClass.getCanonicalName
      else
        null
    }
  }
  class InterpreterHandler(val interpreter: IMain) {
    val wrapper = new ZtoolsInterpreterWrapper(interpreter)

    def getVariableNames: immutable.Seq[String] =
      interpreter.definedSymbolList.filter { x => x.isGetter }.map(_.name.toString).distinct

    def getInfo(name: String, tpe: String): ScalaVariableInfo = {
      val obj = valueOfTerm(name).orNull
      ScalaVariableInfo(isAccessible = true, isLazy = false, obj, tpe, name, null)
    }

    def valueOfTerm(id: String): Option[Any] = wrapper.valueOfTerm(id)
  }

  case class ScalaVariableInfo(isAccessible: Boolean,
                               isLazy: Boolean,
                               value: Any,
                               tpe: String,
                               path: String,
                               ref: String) {
    val name: String = if (path != null)
      path.substring(path.lastIndexOf('.') + 1)
    else
      null
  }



  //noinspection TypeAnnotation
  class ZtoolsInterpreterWrapper(val iMain: IMain) {

    import scala.language.implicitConversions
    import scala.reflect.runtime.{universe => ru}
    import iMain.global._

    import scala.util.{Try => Trying}

    private lazy val importToGlobal = iMain.global mkImporter ru
    private lazy val importToRuntime = ru.internal createImporter iMain.global

    private implicit def importFromRu(sym: ru.Symbol) = importToGlobal importSymbol sym

    private implicit def importToRu(sym: Symbol): ru.Symbol = importToRuntime importSymbol sym

    // see https://github.com/scala/scala/pull/5852/commits/a9424205121f450dea2fe2aa281dd400a579a2b7
    def valueOfTerm(id: String): Option[Any] = exitingTyper {
      def fixClassBasedFullName(fullName: List[String]): List[String] = {
        if (settings.Yreplclassbased.value) {
          val line :: read :: rest = fullName
          line :: read :: "INSTANCE" :: rest
        } else fullName
      }

      def value(fullName: String) = {
        val universe = iMain.runtimeMirror.universe
        import universe.{InstanceMirror, Symbol, TermName}
        val pkg :: rest = fixClassBasedFullName((fullName split '.').toList)
        val top = iMain.runtimeMirror.staticPackage(pkg)

        @annotation.tailrec
        def loop(inst: InstanceMirror, cur: Symbol, path: List[String]): Option[Any] = {
          def mirrored =
            if (inst != null) inst
            else iMain.runtimeMirror reflect (iMain.runtimeMirror reflectModule cur.asModule).instance

          path match {
            case last :: Nil =>
              cur.typeSignature.decls find (x => x.name.toString == last && x.isAccessor) map { m =>
                (mirrored reflectMethod m.asMethod).apply()
              }
            case next :: rest =>
              val s = cur.typeSignature.member(TermName(next))
              val i =
                if (s.isModule) {
                  if (inst == null) null
                  else iMain.runtimeMirror reflect (inst reflectModule s.asModule).instance
                }
                else if (s.isAccessor) {
                  iMain.runtimeMirror reflect (mirrored reflectMethod s.asMethod).apply()
                }
                else {
                  assert(false, s.fullName)
                  inst
                }
              loop(i, s, rest)
            case Nil => None
          }
        }

        loop(null, top, rest)
      }

      Option(iMain.symbolOfTerm(id)) filter (_.exists) flatMap (s => Trying(value(s.fullName)).toOption.flatten)
    }
  }

  class ReferenceManager {
    private val refMap = mutable.Map[ReferenceWrapper, String]()
    private val refInvMap = new TrieMap[ReferenceWrapper]()

    /**
     * Returns a reference (e.g. valid path) to the object or creates a record in reference maps (and returns null).
     *
     * @param obj  an object we want to find a reference for (can be null)
     * @param path path of the object e.g. myVar.myField.b
     * @return reference path to the object obj. The method returns null if obj is null itself or
     *         obj hasn't been mentioned earlier or in the case of AnyVal object.
     */
    def getRef(obj: Any, path: String): String = obj match {
      case null | _: Unit =>
        clearRefIfPathExists(path)
        null
      case ref: AnyRef =>
        val wrapper = new ReferenceWrapper(ref)
        if (refMap.contains(wrapper)) {
          if (refInvMap.get(path).orNull != wrapper) clearRefIfPathExists(path)
          refMap(wrapper)
        } else {
          clearRefIfPathExists(path)
          refMap(wrapper) = path
          refInvMap.put(path, wrapper)
          null
        }
      case _ => null
    }


    private def clearRefIfPathExists(path: String): Unit = {
      if (refInvMap.contains(path)) {
        val tree = refInvMap.subtree(path)
        tree.forEach(refMap.remove(_: ReferenceWrapper))
      }
    }
  }

  class ReferenceWrapper(val ref: AnyRef) {
    override def hashCode(): Int = ref.hashCode()

    override def equals(obj: Any): Boolean = obj match {
      case value: ReferenceWrapper =>
        ref.eq(value.ref)
      case _ => false
    }
  }


  class VariablesView(val intp: IMain,
                      val timeout: Int,
                      val variableTimeout: Int,
                      val collectionSizeLimit: Int,
                      val stringSizeLimit: Int,
                      val blackList: List[String],
                      val whiteList: List[String] = null,
                      val filterUnitResults: Boolean,
                      val enableProfiling: Boolean,
                      val depth: Int,
                      val interpreterResCountLimit: Int = 5) {
    val errors: mutable.MutableList[String] = mutable.MutableList[String]()
    private val interpreterHandler = new InterpreterHandler(intp)
    private val referenceManager = new ReferenceManager()

    private val touched = mutable.Map[String, ScalaVariableInfo]()

    private val handlerManager = new HandlerManager(
      collectionSizeLimit = collectionSizeLimit,
      stringSizeLimit = stringSizeLimit,
      timeout = variableTimeout,
      referenceManager = referenceManager,
      enableProfiling = enableProfiling
    )

    //noinspection ScalaUnusedSymbol
    def getZtoolsJsonResult: String = {
      implicit val ztoolsFormats: AnyRef with Formats = Serialization.formats(NoTypeHints)
      Serialization.write(
        Map(
          "variables" -> resolveVariables,
          "errors" -> (errors ++ handlerManager.getErrors)
        )
      )
    }

    def toJson: String = {
      implicit val ztoolsFormats: AnyRef with Formats = Serialization.formats(NoTypeHints)
      Serialization.write(resolveVariables)
    }

    def resolveVariables: mutable.Map[String, Any] = {
      val result: mutable.Map[String, Any] = mutable.Map[String, Any]()
      val startTime = System.currentTimeMillis()

      val interpreterVariablesNames = interpreterHandler.getVariableNames
      val finalNames = filterVariableNames(interpreterVariablesNames)

      finalNames.foreach { name =>
        val varType = interpreterHandler.interpreter.typeOfTerm(name).toString().stripPrefix("()")
        val variable = mutable.Map[String, Any]()

        result += name -> variable
        variable += ResNames.TYPE -> varType
        if (!isUnitOrNullResult(result, name))
          variable += ResNames.VALUE -> "<Not calculated>"
      }

      var passedVariablesCount = 0
      val totalVariablesCount = finalNames.size

      if (checkTimeout(startTime, passedVariablesCount, totalVariablesCount))
        return result

      finalNames.foreach { name =>
        if (checkTimeout(startTime, passedVariablesCount, totalVariablesCount))
          return result
        passedVariablesCount += 1

        if (!isUnitOrNullResult(result, name)) {

          calculateVariable(result, name)
        }
      }
      result
    }

    private def calculateVariable(result: mutable.Map[String, Any], name: String) = {
      val valMap = result(name).asInstanceOf[mutable.Map[String, Any]]
      try {
        val startTime = System.currentTimeMillis()

        val info = interpreterHandler.getInfo(name, valMap(ResNames.TYPE).asInstanceOf[String])
        val ref = referenceManager.getRef(info.value, name)
        touched(info.path) = info

        if (ref != null && ref != info.path) {
          result += (info.path -> mutable.Map[String, Any](ResNames.REF -> ref))
        } else {
          result += info.path -> parseInfo(info, depth, startTime)
        }
      } catch {
        case t: Throwable =>
          valMap += ResNames.VALUE -> ExceptionUtils.getRootCauseMessage(t)
      }
    }

    private def isUnitOrNullResult(result: mutable.Map[String, Any], name: String) = {
      val res = result(name).asInstanceOf[mutable.Map[String, Any]]
      val valType = res(ResNames.TYPE)
      valType == "Unit" || valType == "Null"
    }

    def resolveVariable(path: String): mutable.Map[String, Any] = {
      val result = mutable.Map[String, Any]()
      val obj = touched.get(path).orNull
      if (obj.ref != null) {
        result += (ResNames.VALUE -> mutable.Map[String, Any](ResNames.REF -> obj.ref))
      } else {
        result += (ResNames.VALUE -> parseInfo(obj, depth))
      }
      result
    }

    private def parseInfo(info: ScalaVariableInfo, depth: Int, startTime: Long = System.currentTimeMillis()): Any = {
      val loopback = new Loopback {
        override def pass(obj: Any, id: String): Any = {
          val si = ScalaVariableInfo(isAccessible = true, isLazy = false, obj, null, id, referenceManager.getRef(obj, id))
          parseInfo(si, depth - 1)
        }
      }
      handlerManager.handleVariable(info, loopback, depth, startTime)
    }

    private def filterVariableNames(interpreterVariablesNames: Seq[String]) = {
      val variablesNames = interpreterVariablesNames.seq
        .filter { name => !blackList.contains(name) }
        .filter { name => whiteList == null || whiteList.contains(name) }


      val p = Pattern.compile("res\\d*")
      val (resVariables, otherVariables: immutable.Seq[String]) = variablesNames.partition(x => p.matcher(x).matches())
      val sortedResVariables = resVariables
        .map(res => Try(res.stripPrefix("res").toInt))
        .filter(_.isSuccess)
        .map(_.get)
        .sortWith(_ > _)
        .take(interpreterResCountLimit)
        .map(num => "res" + num)

      val finalNames = otherVariables ++ sortedResVariables
      finalNames
    }

    //noinspection ScalaUnusedSymbol
    private implicit def toJavaFunction[A, B](f: A => B): JFunction[A, B] = new JFunction[A, B] {
      override def apply(a: A): B = f(a)
    }

    private def checkTimeout(startTimeout: Long, passed: Int, total: Int): Boolean = {
      val isTimeoutExceed = System.currentTimeMillis() - startTimeout > timeout
      if (isTimeoutExceed)
        errors += s"Variables collect timeout. Exceed ${timeout}ms. Parsed $passed from $total."
      isTimeoutExceed
    }
  }

  class DatasetHandler extends AbstractTypeHandler {
    override def accept(obj: Any): Boolean = obj.isInstanceOf[Dataset[_]]

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] = {
      val obj = scalaInfo.value
      val df = obj.asInstanceOf[Dataset[_]]


      val schema = df.schema
      val jsonSchemaColumns = schema.fields.map(field => {
        val value = withJsonObject { jsonField =>
          jsonField += "name" -> wrap(field.name, null)
          jsonField += "nullable" -> wrap(field.nullable, null)
          jsonField += "dataType" -> wrap(field.dataType.typeName, null)
        }
        wrap(value, "org.apache.spark.sql.types.StructField")
      }
      )

      val jsonSchema = mutable.Map(
        ResNames.VALUE -> jsonSchemaColumns,
        ResNames.TYPE -> "org.apache.spark.sql.types.StructType",
        ResNames.LENGTH -> jsonSchemaColumns.length
      )

      val dfValue = mutable.Map(
        "schema()" -> jsonSchema,
        "getStorageLevel()" -> wrap(df.storageLevel.toString(), "org.apache.spark.storage.StorageLevel")
      )

      mutable.Map(
        ResNames.VALUE -> dfValue
      )
    }
  }


  class RDDHandler extends AbstractTypeHandler {
    override def accept(obj: Any): Boolean = obj.isInstanceOf[RDD[_]]

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] = withJsonObject {
      json =>
        val obj = scalaInfo.value
        val rdd = obj.asInstanceOf[RDD[_]]
        json += (ResNames.VALUE -> withJsonObject { value =>
          value += ("getNumPartitions()" -> wrap(rdd.getNumPartitions, "Int"))
          value += ("name" -> wrap(rdd.name, "String"))
          value += ("id" -> wrap(rdd.id, "Int"))
          value += ("partitioner" -> wrap(rdd.partitioner.toString, "Option[org.apache.spark.Partitioner]"))
          value += ("getStorageLevel()" -> wrap(rdd.getStorageLevel.toString, "org.apache.spark.storage.StorageLevel"))
        })
    }
  }

  class SparkContextHandler extends AbstractTypeHandler {
    override def accept(obj: Any): Boolean = obj.isInstanceOf[SparkContext]

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] = withJsonObject {
      json =>
        val sc = scalaInfo.value.asInstanceOf[SparkContext]
        json += (ResNames.VALUE -> withJsonObject { json =>
          json += ("sparkUser" -> wrap(sc.sparkUser, "String"))
          json += ("sparkTime" -> wrap(sc.startTime, "Long"))
          json += ("applicationId()" -> wrap(sc.applicationId, "String"))
          json += ("applicationAttemptId()" -> wrap(sc.applicationAttemptId.toString, "Option[String]"))
          json += ("appName()" -> sc.appName)
        })
    }
  }

  class SparkSessionHandler extends AbstractTypeHandler {
    override def accept(obj: Any): Boolean = obj.isInstanceOf[SparkSession]

    override def handle(scalaInfo: ScalaVariableInfo, loopback: Loopback, depth: Int): mutable.Map[String, Any] = withJsonObject {
      json =>
        val obj = scalaInfo.value
        val id = scalaInfo.path

        val spark = obj.asInstanceOf[SparkSession]
        json += (ResNames.VALUE -> withJsonObject { json =>
          json += ("version()" -> spark.version)
          json += ("sparkContext" -> loopback.pass(spark.sparkContext, s"$id.sparkContext"))
        })
    }
  }


  /**
   * Main section
   */
  val iMain: IMain = $intp
  val depth: Int = %s
  val filterUnitResults: Boolean = true
  val enableProfiling: Boolean = %s
  val collectionSizeLimit = %s
  val stringSizeLimit = %s
  val timeout = %s
  val variableTimeout = %s
  val interpreterResCountLimit = %s
  val blackList = "$intp,sqlContext,z,engine".split(',').toList
  val whiteList: List[String] =  %s


  val variableView = new VariablesView(
    intp = iMain,
    timeout = timeout,
    variableTimeout = variableTimeout,
    collectionSizeLimit = collectionSizeLimit,
    stringSizeLimit = stringSizeLimit,
    blackList = blackList,
    whiteList = whiteList,
    filterUnitResults = filterUnitResults,
    enableProfiling = enableProfiling,
    depth = depth,
    interpreterResCountLimit = interpreterResCountLimit
  )

  implicit val ztoolsFormats: AnyRef with Formats = Serialization.formats(NoTypeHints)
  val variablesJson = variableView.getZtoolsJsonResult
  println("---ztools-scala---")
  println(variablesJson)
  println("---ztools-scala---")
}
catch {
  case t: Throwable =>
    import org.apache.commons.lang.exception.ExceptionUtils
    import org.json4s.jackson.Serialization
    import org.json4s.{Formats, NoTypeHints}

    implicit val ztoolsFormats: AnyRef with Formats = Serialization.formats(NoTypeHints)
    val result = Serialization.write(Map(
      "errors" -> Array(f"${ExceptionUtils.getMessage(t)}\n${ExceptionUtils.getStackTrace(t)}")
    ))
    println("---ztools-scala---")
    println(result)
    println("---ztools-scala---")
}