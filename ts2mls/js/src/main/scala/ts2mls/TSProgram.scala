package ts2mls

import scala.scalajs.js
import js.DynamicImplicits._
import ts2mls.types._
import scala.collection.mutable.{HashSet, HashMap}

// for general ts, we still consider that there is a top-level module
// and in mls we will import ts file like this:
// import * as TopLevelModuleName from "filename"
// for es5.d.ts, we only need to translate everything
// and it will be imported without top-level module before we compile other files
class TSProgram(filename: String, uesTopLevelModule: Boolean) {
  private val program = TypeScript.createProgram(Seq(filename))
  private val cache = new HashMap[String, TSNamespace]()

  // check if file exists
  if (!program.fileExists(filename)) throw new Exception(s"file ${filename} doesn't exist.")

  implicit val checker = TSTypeChecker(program.getTypeChecker())

  def generate(workDir: String, targetPath: String): Unit = generate(filename, workDir, targetPath)(List())

  def generate(filename: String, workDir: String, targetPath: String)(implicit stack: List[String]): Unit = {
    val globalNamespace = TSNamespace()
    val sourceFile = TSSourceFile(program.getSourceFile(filename), globalNamespace)
    val importList = sourceFile.getImportList
    val absName = TSModuleResolver.resolve(filename)
    cache.addOne(absName, globalNamespace)

    val moduleName = TSImport.getModuleName(filename, false)
    val relatedPath =
      if (filename.startsWith(workDir)) filename.substring(workDir.length() + 1, filename.lastIndexOf('/') + 1)
      else throw new AssertionError(s"wrong work dir $workDir")

    def toTSFile(imp: TSImport) = // TODO: node_modules
      if (!imp.filename.endsWith(".ts")) s"${imp.filename}.ts"
      else imp.filename
    val (cycleList, otherList) = importList.partition(imp => stack.contains(toTSFile(imp)))

    otherList.foreach(imp => {
      val newFilename = toTSFile(imp)
      generate(newFilename, TSModuleResolver.resolve(workDir), targetPath)(absName :: stack)
    })

    var writer = JSWriter(s"$targetPath/$relatedPath/$moduleName.mlsi")
    val imported = new HashSet[String]()
    
    otherList.foreach(imp => {
      val name = TSImport.getModuleName(imp.`import`, true)
      if (!imported(name)) {
        imported += name
        writer.writeln(s"""import "$name.mlsi"""")
      }
    })
    cycleList.foreach(imp => {
      val newFilename = toTSFile(imp)
      writer.writeln(s"export declare module ${TSImport.getModuleName(imp.`import`, false)} {")
      cache(newFilename).generate(writer, "  ")
      writer.writeln("}")
    })

    generate(writer, otherList, globalNamespace, moduleName)

    writer.close()
  }

  private def generate(writer: JSWriter, importList: List[TSImport], globalNamespace: TSNamespace, moduleName: String): Unit =
    if (!uesTopLevelModule) globalNamespace.generate(writer, "") // will be imported directly and has no dependency
    else {
      writer.writeln(s"export declare module $moduleName {")
      globalNamespace.generate(writer, "  ")
      writer.writeln("}")
    }
}

object TSProgram {
  def apply(filename: String, uesTopLevelModule: Boolean) = new TSProgram(filename, uesTopLevelModule)
}
