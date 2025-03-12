import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}
import scala.language.postfixOps
import sys.process._
import java.nio.file.{Files, Path, Paths}

object Utils {
  type CompiledProgram = (Array[Int], Array[Int])

  def readAndSplitIntoTextAndData(file: Path): CompiledProgram = {
    val textSectionBin = Files.createTempFile("wildcat_text", ".bin")
    val dataSectionBin = Files.createTempFile("wildcat_data", ".bin")

    f"riscv64-unknown-elf-objcopy -O binary -j .text $file $textSectionBin".!
    f"riscv64-unknown-elf-objcopy -O binary -j .data $file $dataSectionBin".!

    val textSection = Files.readAllBytes(textSectionBin)
    val dataSection = Files.readAllBytes(dataSectionBin)

    val textSectionArray = new Array[Int](textSection.length / 4 + 1)
    val dataSectionArray = new Array[Int](dataSection.length / 4 + 1)

    // Print result as hex
    for (i <- 0 until textSection.length) {
      textSectionArray(i / 4) = textSectionArray(i / 4) | (textSection(i) << (8 * (i % 4)))
    }

    for (i <- 0 until dataSection.length) {
      dataSectionArray(i / 4) = dataSectionArray(i / 4) | (dataSection(i) << (8 * (i % 4)))
    }

    Files.deleteIfExists(textSectionBin)
    Files.deleteIfExists(dataSectionBin)
    Files.deleteIfExists(file)

    (textSectionArray, dataSectionArray)
  }

  def compiledProgramCacheLookup(source: String): Option[CompiledProgram] = {
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

  def writeCompiledProgramCache(source: String, p: CompiledProgram): Unit = {
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

  def resolveResourceAsTemp(resource: String): Path = {
    // Use extension of resource as file extension
    val extension = resource.split('.').last
    val tempFile = Files.createTempFile("wildcat_resource", s".$extension")
    Files.copy(getClass.getResource(resource).openStream(), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    tempFile
  }
}

object InlineWildcatCProgram {
  // Take a C program and compile it with gnu tool chain, get output as 4-byte instructions
  def apply(c: String): Utils.CompiledProgram = {
    if (Utils.compiledProgramCacheLookup(c).isDefined) {
      return Utils.compiledProgramCacheLookup(c).get
    }

    val sourceFile = Files.createTempFile("wildcat_source", ".c")
    Files.write(sourceFile, c.getBytes)

    // Get path to ressources crt0.c and linker.ld
    val crt0 = Utils.resolveResourceAsTemp("/dyn_compilation/crt0.c")
    val linkerFile = Utils.resolveResourceAsTemp("/dyn_compilation/linker.ld")

    val crt0ObjPath = Files.createTempFile("wildcat_crt0", ".o")
    val sourceObjPath = Files.createTempFile("wildcat_source", ".o")
    val aoutPath = Files.createTempFile("wildcat_result", ".out")

    f"riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 $crt0 -c -o $crt0ObjPath".!
    f"riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 $sourceFile -c -o $sourceObjPath".!
    f"riscv64-unknown-elf-ld -melf32lriscv -T $linkerFile $crt0ObjPath $sourceObjPath -o $aoutPath".!

    Files.deleteIfExists(crt0)
    Files.deleteIfExists(sourceFile)
    Files.deleteIfExists(linkerFile)
    Files.deleteIfExists(crt0ObjPath)
    Files.deleteIfExists(sourceObjPath)

    val p = Utils.readAndSplitIntoTextAndData(aoutPath)
    Utils.writeCompiledProgramCache(c, p)
    p
  }
}

object InlineWildcatASMProgram {
  def apply(asmProgram: String): Utils.CompiledProgram = {
    if (Utils.compiledProgramCacheLookup(asmProgram).isDefined) {
      return Utils.compiledProgramCacheLookup(asmProgram).get
    }

    val sourceFile = Files.createTempFile("wildcat_source", ".S")
    Files.write(sourceFile, asmProgram.getBytes)

    val objectPath = Files.createTempFile("wildcat_object", ".o")

    f"riscv64-unknown-elf-as -march=rv32i -mabi=ilp32 $sourceFile -o $objectPath".!

    Files.deleteIfExists(sourceFile)

    val p = Utils.readAndSplitIntoTextAndData(objectPath)
    Utils.writeCompiledProgramCache(asmProgram, p)
    p
  }
}

object TestTRISCVCompiler extends App {
  InlineWildcatCProgram(
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

  val p = InlineWildcatASMProgram(
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