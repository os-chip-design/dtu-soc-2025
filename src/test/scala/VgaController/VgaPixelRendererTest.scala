import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random // Import Random

// Assuming VgaConfig object is defined and accessible
import VgaConfig._

class VgaPixelRendererTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "VgaPixelRenderer"

  // Replicate the palette logic here for expectation calculation
  val testColorPalette = Seq(
    // R    G    B
    (0x0, 0x0, 0x0), // 0 Black
    (0x0, 0x0, 0xa), // 1 Blue
    (0x0, 0xa, 0x0), // 2 Green
    (0x0, 0xa, 0xa), // 3 Cyan
    (0xa, 0x0, 0x0), // 4 Red
    (0xa, 0x0, 0xa), // 5 Magenta
    (0xa, 0x5, 0x0), // 6 Brown
    (0xa, 0xa, 0xa), // 7 Light Gray
    (0x5, 0x5, 0x5), // 8 Dark Gray
    (0x5, 0x5, 0xf), // 9 Light Blue
    (0x5, 0xf, 0x5), // A Light Green
    (0x5, 0xf, 0xf), // B Light Cyan
    (0xf, 0x5, 0x5), // C Light Red
    (0xf, 0x5, 0xf), // D Light Magenta
    (0xf, 0xf, 0x5), // E Yellow
    (0xf, 0xf, 0xf) // F White
  )

  it should "output the correct colors based on attribute and pixel data" in {
    test(new VgaPixelRenderer).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Example: White foreground (index F), Blue background (index 1)
      // Attribute byte: Blink=0, BG=001, FG=1111 => 00011111 binary = 0x1F
      dut.activeArea.poke(true.B)
      val attribute = "h1F".U(8.W)
      dut.io.attributeData.poke(attribute)

      // Font glyph data for testing
      val fontPixelData = "b01100110".U(CHAR_WIDTH_PIXELS.W) // Example pattern
      dut.io.pixelData.poke(fontPixelData)

      val (fgR, fgG, fgB) = testColorPalette(0xf) // White
      val (bgR, bgG, bgB) = testColorPalette(0x1) // Blue

      for (index <- 0 until CHAR_WIDTH_PIXELS) {
        dut.io.xIndex.poke(index.U)
        val bitPosition = (CHAR_WIDTH_PIXELS - 1) - index
        val expectedPixelVal = (fontPixelData.litValue >> bitPosition) & 1

        // Determine expected RGB output
        val expectedR = if (expectedPixelVal == 1) fgR else bgR
        val expectedG = if (expectedPixelVal == 1) fgG else bgG
        val expectedB = if (expectedPixelVal == 1) fgB else bgB

        // Check outputs
        dut.io.r.expect(expectedR.U(4.W))
        dut.io.g.expect(expectedG.U(4.W))
        dut.io.b.expect(expectedB.U(4.W))

        // Clock step can be used to generate VCD
        // dut.clock.step(1)
      }
    }
  }

  it should "calculate the correct values for random indexes" in {
    test(new VgaPixelRenderer).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.activeArea.poke(true.B)
      val rng = new Random(0) // Use fixed seed for reproducibility
      val repeats = 100 // How many times to test with random values

      for (_ <- 0 until repeats) {
        // Generate random inputs
        val randomPixelData = BigInt(CHAR_WIDTH_PIXELS, rng)
        val randomAttribute = BigInt(8, rng)
        val randomIndex = rng.nextInt(CHAR_WIDTH_PIXELS)

        dut.io.pixelData.poke(randomPixelData.U)
        dut.io.attributeData.poke(randomAttribute.U)
        dut.io.xIndex.poke(randomIndex.U)

        // Calculate expected outputs based on the same logic as the DUT
        val fgIndex = randomAttribute & 0xf
        val bgIndex = (randomAttribute >> 4) & 0x7

        val (fgR, fgG, fgB) = testColorPalette(fgIndex.toInt)
        val (bgR, bgG, bgB) = testColorPalette(bgIndex.toInt)

        val bitPosition = (CHAR_WIDTH_PIXELS - 1) - randomIndex
        val expectedPixelVal = (randomPixelData >> bitPosition) & 1

        val expectedR = if (expectedPixelVal == 1) fgR else bgR
        val expectedG = if (expectedPixelVal == 1) fgG else bgG
        val expectedB = if (expectedPixelVal == 1) fgB else bgB

        // Check outputs
        dut.io.r.expect(expectedR.U(4.W))
        dut.io.g.expect(expectedG.U(4.W))
        dut.io.b.expect(expectedB.U(4.W))
        dut.clock.step(1)
      }
    }
  }
}
