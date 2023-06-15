package ts2mls

import org.scalatest.funsuite.AnyFunSuite
import scala.collection.immutable
import TSPathResolver.basename
import ts2mls.TSPathResolver

class TSTypeGenerationTest extends AnyFunSuite {
  import TSTypeGenerationTest._

  testsData.foreach((filename) => test(filename) {
    val program = TSProgram(
      filename,
      "./ts2mls/js/src/test/typescript",
      !directlyImportedSet.contains(filename),
      None
    )
    program.generate(None, s"ts2mls/js/src/test/diff/${basename(filename)}.mlsi")
  })
}

object TSTypeGenerationTest {
  private val testsData = List(
    "./Array.ts",
    "./BasicFunctions.ts",
    "./ClassMember.ts",
    "./Cycle1.ts",
    "./Dec.d.ts",
    "./Dom.d.ts",
    "./Enum.ts",
    "./ES5.d.ts",
    "./Export.ts",
    "./Heritage.ts",
    "./HighOrderFunc.ts",
    "./Import.ts",
    "./InterfaceMember.ts",
    "./Intersection.ts",
    "./Literal.ts",
    "./Namespace.ts",
    "./Optional.ts",
    "./Overload.ts",
    "./Tuple.ts",
    "./Type.ts",
    "./TypeParameter.ts",
    "./Union.ts",
    "./Variables.ts",
  )

  private val directlyImportedSet = Set[String]("./ES5.d.ts", "./Dom.d.ts")
}
