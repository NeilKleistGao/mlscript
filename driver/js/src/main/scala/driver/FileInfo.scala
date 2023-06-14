package driver

import ts2mls.{TSModuleResolver, TypeScript, TSImport}

final case class FileInfo(
  workDir: String, // work directory (related to compiler path)
  localFilename: String, // filename (related to work dir, or in node_modules)
  interfaceDir: String, // .mlsi file directory (related to output dir)
) {
  import TSModuleResolver.{normalize, isLocal, dirname, basename}

  val relatedPath: Option[String] = // related path (related to work dir, or none if it is in node_modules)
    if (isLocal(localFilename)) Some(normalize(dirname(localFilename)))
    else None

  val isNodeModule: Boolean = relatedPath.isEmpty

  // module name in ts/mls
  val moduleName = basename(localFilename)

  // full filename (related to compiler path, or in node_modules)
  lazy val filename: String =
    if (!isNodeModule) normalize(s"./$workDir/$localFilename")
    else localFilename

  val interfaceFilename: String = // interface filename (related to output directory)
    relatedPath.fold(
      s"$interfaceDir/${TSImport.createInterfaceForNode(localFilename)}"
    )(path => s"${normalize(s"$interfaceDir/$path/$moduleName.mlsi")}")
  
  val jsFilename: String =
    relatedPath.fold(moduleName)(path => normalize(s"$path/$moduleName.js"))

  def `import`(path: String): FileInfo =
    if (isLocal(path))
      relatedPath match {
        case Some(value) => FileInfo(workDir, s"./${normalize(s"$value/$path")}", interfaceDir)
        case _ =>
          val currentPath = TSModuleResolver.dirname(TSImport.createInterfaceForNode(localFilename))
          FileInfo(workDir, s"./$currentPath/$path", interfaceDir)
      }
    else FileInfo(workDir, path, interfaceDir)
}

object FileInfo {
  def importPath(filename: String): String =
    if (filename.endsWith(".mls") || filename.endsWith(".ts"))
      filename.replace(TSModuleResolver.extname(filename), ".mlsi")
    else filename + ".mlsi"
}
