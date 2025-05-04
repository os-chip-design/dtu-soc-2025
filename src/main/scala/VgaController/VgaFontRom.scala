import chisel3._
import chisel3.util._
import java.nio.file.{Files, Paths, NoSuchFileException}
import java.io.IOException

import VgaConfig._

/** IO Bundle for the VgaFontRom.
  */
class VgaFontRomIo extends Bundle {
  // Character code to look up (Unless it's some weird font, should be ASCII)
  val charCode = Input(UInt(8.W)) // (Hopefully) ASCII 8-bit character code
  // Vertical index (row) within the character glyph
  val glyphY = Input(UInt(CHAR_Y_INDEX_BITS.W))
  // Output row of pixel data for the character glyph
  // This is output the same clock cycle
  val pixelData = Output(UInt(CHAR_WIDTH_PIXELS.W))
}

/** Font ROM that loads its data from a raw binary file. Dimensions (width,
  * height) are taken from VgaConfig. halts with an exception if the font file
  * cannot be loaded or has the wrong size based on the config dimensions.
  *
  * @param fontFilePath
  *   Path to the raw binary font data file. The expected size depends on
  *   CHAR_WIDTH_PIXELS and CHAR_HEIGHT_PIXELS from VgaConfig. Example:
  *   "src/main/scala/VgaController/fonts/VGA8.F16" (expects 4096 bytes if
  *   config is 8x16). Has not been tested for any other size, but you can find
  *   other cool fonts online
  */
class VgaFontRom(fontFilePath: String) extends Module { // Removed width/height params

  val io = IO(new VgaFontRomIo())

  val numEntries = 256 * CHAR_HEIGHT_PIXELS // Total rows for 256 characters
  // For standard 8-pixel wide fonts, expectedBytes = numEntries, which is nice.
  require(
    CHAR_WIDTH_PIXELS == 8,
    "VgaFontRom currently assumes 8-bit wide font data (1 byte per row)"
  )
  val expectedBytes = numEntries

  /** Loads font data from a binary file. Throws an exception if the file cannot
    * be read or has incorrect size.
    */
  def loadFontDataFromBinary(
      filePath: String,
      expectedSize: Int,
      outputWidth: Int
  ): Seq[UInt] = {

    val byteArray: Array[Byte] =
      try {
        Files.readAllBytes(Paths.get(filePath))
      } catch {
        case e: NoSuchFileException =>
          throw new RuntimeException(
            s"Error: Font file not found at '$filePath'",
            e
          )
        case e: IOException =>
          throw new RuntimeException(
            s"Error: Could not read font file '$filePath'",
            e
          )
      }

    if (byteArray.length != expectedSize) {
      throw new IllegalArgumentException(
        s"Error: Font file '$filePath' has incorrect size based on VgaConfig (${CHAR_WIDTH_PIXELS}x${CHAR_HEIGHT_PIXELS}). " +
          s"Expected $expectedSize bytes, but found ${byteArray.length} bytes."
      )
    }

    byteArray
      .map(byte =>
        (byte.toInt & 0xff)
          .U(outputWidth.W) // Use outputWidth for the UInt type
      ) // Use & 0xFF to treat as unsigned
      .toSeq
  }

  // Load data using dimensions from config
  val fontDataLoaded: Seq[UInt] =
    loadFontDataFromBinary(fontFilePath, expectedBytes, CHAR_WIDTH_PIXELS)

  // Create the VecInit ROM structure using the loaded data
  val fontDataVec = VecInit(fontDataLoaded)

  // Calculate ROM address based on character code and glyph row index
  val romAddr = (io.charCode * CHAR_HEIGHT_PIXELS.U) + io.glyphY
  io.pixelData := fontDataVec(romAddr)
}

object VgaFontRom extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(
      new VgaFontRom("src/main/scala/VgaController/fonts/VGA8.F16")
    )
}
