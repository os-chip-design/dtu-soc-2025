import sys.process._
import scala.util.chaining._
import scala.collection.mutable.ListBuffer
import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.file.{Files, Path, Paths}

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

  private def readAndSplitIntoTextAndData(file: Path): CompiledProgram = Deferable { defer =>
    val textSectionBin = Files.createTempFile("wildcat_text", ".bin")

    defer {
      Files.deleteIfExists(textSectionBin)
    }

    val dataSectionBin = Files.createTempFile("wildcat_data", ".bin")

    defer {
      Files.deleteIfExists(dataSectionBin)
    }

    f"riscv64-unknown-elf-objcopy -O binary -j .text $file $textSectionBin".!
    f"riscv64-unknown-elf-objcopy -O binary -j .data $file $dataSectionBin".!

    val textSection = Files.readAllBytes(textSectionBin)
    val dataSection = Files.readAllBytes(dataSectionBin)

    // In a pure functional way group the text section by four bytes and convert to int
    val textSectionArray = textSection.grouped(4).map(_.foldRight(0)((x, acc) => (acc << 8) | (x & 0xff))).toArray
    val dataSectionArray = dataSection.grouped(4).map(_.foldRight(0)((x, acc) => (acc << 8) | (x & 0xff))).toArray

    // Print all as hex
    textSectionArray.foreach(x => println(f"${x}%08x"))
    println("----")
    dataSectionArray.foreach(x => println(f"${x}%08x"))

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

    // Get path to ressources crt0.c and linker.ld
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

    f"riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 $crt0 -c -o $crt0ObjPath".!
    f"riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 $sourceFile -c -o $sourceObjPath".!
    f"riscv64-unknown-elf-ld -melf32lriscv -T $linkerFile $crt0ObjPath $sourceObjPath -o $aoutPath".!

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

    f"riscv64-unknown-elf-as -march=rv32i -mabi=ilp32 $sourceFile -o $objectPath".!

    readAndSplitIntoTextAndData(objectPath).tap {
      writeCompiledProgramCache(asmProgram, _)
    }
  }

  def emptyProgram: CompiledProgram = {
    (Array(0), Array(0))
  }
}

object TestTRISCVCompiler extends App {
  RISCVCompiler.inlineASM(
    """.text
addi x1, x0, 42
sw x1, 0(x0)
"""
  )

  RISCVCompiler.inlineC(
    """
int main() {
  // UART on 0xf0000004: status and output
  volatile int *ptr = (int *) 0xf0000004;
  char *str = "Hello World!";
  for (int i = 0; i < 12; i++) {
    *ptr = str[i];
  }
}

    """)

  val p = RISCVCompiler.inlineASM(
    """
.global _start
.text

_start:                    /* x0  = 0    0x000 */
    /* Test ADDI */
    addi x1 , x0,   1000  /* x1  = 1000 0x3E8 */
    addi x2 , x1,   2000  /* x2  = 3000 0xBB8 */
    addi x3 , x2,  -1000  /* x3  = 2000 0x7D0 */
    addi x4 , x3,  -2000  /* x4  = 0    0x000 */
    addi x5 , x4,   1000  /* x5  = 1000 0x3E8 */

    la x6, variable
    addi x6, x6, 4

.data
variable:
	.word 0xdeadbeef
                    
    """
  )
}