import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable.ListBuffer
import scala.sys.process._
import scala.util.chaining._

private trait Deferable {
  private val stack = ListBuffer.empty[() => Unit]

  def defer(f: => Unit): Unit = {
    stack += (() => f)
  }

  private[Deferable] def exit(): Unit = {
    stack.foreach(f => f())
  }
}

private object Deferable {
  def apply[T](f: ((=> Unit) => Unit) => T): T = {
    val d = new Deferable {}
    try {
      f(d.defer)
    } finally {
      d.exit()
    }
  }
}


object RISCVCompiler {
  type CompiledProgram = (Array[Int], Array[Int])

  private def getToolchainPrefix = {
    // Allow overriding the toolchain prefix with an environment variable
    Option(System.getenv("RISCV_PREFIX")) match {
      case Some(prefix) => prefix
      case None => "riscv64-unknown-elf"
    }
  }

  private def readAndSplitIntoTextAndData(file: Path): CompiledProgram = Deferable { defer =>
    val textSectionBin = Files.createTempFile("wildcat_text", ".bin")

    defer {
      Files.deleteIfExists(textSectionBin)
    }

    val dataSectionBin = Files.createTempFile("wildcat_data", ".bin")

    defer {
      Files.deleteIfExists(dataSectionBin)
    }

    val prefix = getToolchainPrefix

    f"$prefix-objcopy -O binary -j .text $file $textSectionBin".!
    f"$prefix-objcopy -O binary -j .data $file $dataSectionBin".!

    val textSection = Files.readAllBytes(textSectionBin)
    val dataSection = Files.readAllBytes(dataSectionBin)
    val textSectionArray = textSection.grouped(4).map(_.foldRight(0)((x, acc) => (acc << 8) | (x & 0xff))).toArray
    val dataSectionArray = dataSection.grouped(4).map(_.foldRight(0)((x, acc) => (acc << 8) | (x & 0xff))).toArray

    (textSectionArray, dataSectionArray)
  }

  private def compiledProgramCacheLookup(source: String): Option[CompiledProgram] = {
    // Cache in source code resources dyn_compilation/cache
    try {
      val cacheFile = getClass.getResource(f"/dyn_compilation/cache/${source.hashCode}.bin").getPath
      if (Files.exists(Path.of(cacheFile))) {
        val inputObjectStream = new ObjectInputStream(new FileInputStream(cacheFile))
        val p = inputObjectStream.readObject().asInstanceOf[CompiledProgram]
        inputObjectStream.close()
        Some(p)
      } else {
        None
      }

    } catch {
      case _: Exception => None
    }
  }

  private def writeCompiledProgramCache(source: String, p: CompiledProgram): Unit = {
    // Cache in resources dyn_compilation/cache
    val devCache = Paths.get("src/main/resources").toFile

    if (!devCache.exists()) {
      println(System.getenv())
      println(System.getProperty("user.dir"))
      println("You are not in the right directory to cache compiled programs")
      return
    }

    val cacheFile = s"$devCache/dyn_compilation/cache/${source.hashCode}.bin"

    if (!Files.exists(Path.of(cacheFile))) {
      Files.createDirectories(Path.of(cacheFile).getParent)
    }

    val outputObjectStream = new ObjectOutputStream(new FileOutputStream(cacheFile))
    outputObjectStream.writeObject(p)
    outputObjectStream.close()
  }

  private def resolveResourceAsTemp(resource: String): Path = {
    // Use extension of resource as file extension
    val extension = resource.split('.').last
    val tempFile = Files.createTempFile("wildcat_resource", s".$extension")
    Files.copy(getClass.getResource(resource).openStream(), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    tempFile
  }

  // Take a C program and compile it with gnu tool chain, get output as 4-byte instructions
  def inlineC(c: String): CompiledProgram = Deferable { defer =>
    compiledProgramCacheLookup(c) match {
      case Some(p) => return p
      case None =>
    }

    val sourceFile = Files.createTempFile("wildcat_source", ".c")
    defer {
      Files.deleteIfExists(sourceFile)
    }

    Files.write(sourceFile, c.getBytes)

    // Get path to resources crt0.c and linker.ld
    val crt0 = resolveResourceAsTemp("/dyn_compilation/crt0.c")
    defer {
      Files.deleteIfExists(crt0)
    }

    val linkerFile = resolveResourceAsTemp("/dyn_compilation/linker.ld")
    defer {
      Files.deleteIfExists(linkerFile)
    }

    val crt0ObjPath = Files.createTempFile("wildcat_crt0", ".o")
    defer {
      Files.deleteIfExists(crt0ObjPath)
    }

    val sourceObjPath = Files.createTempFile("wildcat_source", ".o")
    defer {
      Files.deleteIfExists(sourceObjPath)
    }

    val aoutPath = Files.createTempFile("wildcat_result", ".out")
    defer {
      Files.deleteIfExists(aoutPath)
    }

    val prefix = getToolchainPrefix

    f"$prefix-gcc -march=rv32i_zicsr -mabi=ilp32 $crt0 -c -o $crt0ObjPath".!
    f"$prefix-gcc -march=rv32i_zicsr -mabi=ilp32 $sourceFile -c -o $sourceObjPath".!
    f"$prefix-ld -melf32lriscv -T $linkerFile $crt0ObjPath $sourceObjPath -o $aoutPath".!

    readAndSplitIntoTextAndData(aoutPath).tap {
      writeCompiledProgramCache(c, _)
    }
  }

  def inlineASM(asmProgram: String): CompiledProgram = Deferable { defer =>
    compiledProgramCacheLookup(asmProgram) match {
      case Some(p) => return p
      case None =>
    }

    val sourceFile = Files.createTempFile("wildcat_source", ".S")

    defer {
      Files.deleteIfExists(sourceFile)
    }

    Files.write(sourceFile, asmProgram.getBytes)

    val objectPath = Files.createTempFile("wildcat_object", ".o")
    defer {
      Files.deleteIfExists(objectPath)
    }

    val prefix = getToolchainPrefix
    f"$prefix-as -march=rv32i_zicsr $sourceFile -o $objectPath".!

    readAndSplitIntoTextAndData(objectPath).tap {
      writeCompiledProgramCache(asmProgram, _)
    }
  }

  def emptyProgram: CompiledProgram = {
    (Array(0), Array(0))
  }
}