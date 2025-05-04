import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.{Files, Paths, NoSuchFileException}
import scala.util.Random

// Test specification for the VgaFontRom module
class VgaFontRomSpec extends AnyFlatSpec with ChiselScalatestTester {

  val testFontWidth = 8
  val testFontHeight = 16
  val testFontFilePath =
    "src/test/scala/VgaController/testFonts/VGA8.F16"

  val fontFileBytes: Array[Byte] =
    try {
      Files.readAllBytes(Paths.get(testFontFilePath))
    } catch {
      case e: NoSuchFileException =>
        fail(
          s"Test font file not found at: $testFontFilePath. Please ensure the file exists.",
          e
        )
      case e: Exception =>
        fail(
          s"Error reading test font file '$testFontFilePath': ${e.getMessage}",
          e
        )
    }

  val expectedFileSize = 256 * testFontHeight
  if (fontFileBytes.length != expectedFileSize) {
    fail(
      s"Test font file '$testFontFilePath' has incorrect size. Expected $expectedFileSize bytes, found ${fontFileBytes.length} bytes."
    )
  }

  behavior of "VgaFontRom Data Reading"

  // Helper function to check memory address
  def checkMemoryAddress(
      dut: VgaFontRom,
      address: Int,
      y: Int,
      expectedValue: BigInt,
      message: String = ""
  ): Unit = {
    dut.io.charCode.poke(address.U)
    dut.io.glyphY.poke(y.U)
    dut.io.pixelData.expect(expectedValue.U(testFontWidth.W), message)
  }

  it should "output correct byte values for a random sample of addresses" in {
    test(new VgaFontRom(testFontFilePath))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        val numSamples =
          30 // It for some reasons takes ages if this is too high
        val rand = new Random()

        for (_ <- 0 until numSamples) {
          // Generate random character code and row
          val randomChar = rand.nextInt(256) // 0 to 255
          val randomRow = rand.nextInt(testFontHeight) // 0 to fontHeight-1
          val fileAddress = randomChar * testFontHeight + randomRow
          val expectedByteValue =
            BigInt(fontFileBytes(fileAddress).toInt & 0xff)
          val failureMsg =
            s"Mismatch at Random Sample: Char $randomChar (0x${randomChar.toHexString}), Row $randomRow: " +
              s"File Address $fileAddress (Expected 0x${expectedByteValue.toString(16)})"

          checkMemoryAddress(
            dut = dut,
            address = randomChar,
            y = randomRow,
            expectedValue = expectedByteValue,
            message = failureMsg
          )

          dut.clock.step(1)
        }
      }
  }

}
