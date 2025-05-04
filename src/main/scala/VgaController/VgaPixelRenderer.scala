import chisel3._
import chisel3.util._

import VgaConfig._

/** IO Bundle for the VgaPixelRenderer. Uses widths derived from VgaConfig.
  */
class VgaPixelRendererIo extends Bundle {
  // Width of the font glyph row data
  val pixelData = Input(UInt(CHAR_WIDTH_PIXELS.W))
  // Attribute data (8 bits: Blink | BG(RGB) | FG(IRGB) )
  val attributeData = Input(UInt(8.W))
  // Index within the font glyph row
  val xIndex = Input(UInt(CHAR_X_INDEX_BITS.W))

  val activeArea = Input(Bool())

  // RGB output (4-bit color depth per channel)
  val r = Output(UInt(4.W))
  val g = Output(UInt(4.W))
  val b = Output(UInt(4.W))
}

/** Renders a single pixel's RGB values based on font glyph data, attribute
  * data, and the pixel's horizontal index within the character glyph.
  * Implements standard 16-color VGA text mode palette (ignoring blink).
  */
class VgaPixelRenderer extends Module {
  val io = IO(new VgaPixelRendererIo)

  // --- Standard VGA 16-Color Palette Lookup ---
  // Each entry is 12 bits: R(4-bits) | G(4-bits) | B(4-bits)
  // Values based on typical VGA hardware mappings This was a bit hard to find
  // information on, so loosely based on ibm enhanced color display.
  // Thanks to chatGPT for generating this nice table <3
  val colorPalette = VecInit(
    Seq(
      // Index (IRGB)  Color         R    G    B     (Hex: RGB)
      "h000".U(12.W), // 0000 (0) Black         0000 0000 0000
      "h00A".U(12.W), // 0001 (1) Blue          0000 0000 1010 (2/3 intensity)
      "h0A0".U(12.W), // 0010 (2) Green         0000 1010 0000
      "h0AA".U(12.W), // 0011 (3) Cyan          0000 1010 1010
      "hA00".U(12.W), // 0100 (4) Red           1010 0000 0000
      "hA0A".U(12.W), // 0101 (5) Magenta       1010 0000 1010
      "hA50".U(
        12.W
      ), // 0110 (6) Brown         1010 0101 0000 (Approximation for Yellow@2/3)
      "hAAA".U(12.W), // 0111 (7) Light Gray    1010 1010 1010
      "h555".U(
        12.W
      ), // 1000 (8) Dark Gray     0101 0101 0101 (Approximation for Black@High Intensity)
      "h55F".U(12.W), // 1001 (9) Light Blue    0101 0101 1111
      "h5F5".U(12.W), // 1010 (A) Light Green   0101 1111 0101
      "h5FF".U(12.W), // 1011 (B) Light Cyan    0101 1111 1111
      "hF55".U(12.W), // 1100 (C) Light Red     1111 0101 0101
      "hF5F".U(12.W), // 1101 (D) Light Magenta 1111 0101 1111
      "hFF5".U(12.W), // 1110 (E) Yellow        1111 1111 0101
      "hFFF".U(12.W) // 1111 (F) White         1111 1111 1111
    )
  )

  // --- Extract Color Indices ---
  val fgIndex = io.attributeData(3, 0)
  val bgIndex = io.attributeData(6, 4)

  // --- Perform Palette Lookup ---
  val fgColorRGB = colorPalette(fgIndex)
  val bgColorRGB = colorPalette(bgIndex) // Background uses first 8 entries

  // Extract individual R, G, B components (4 bits each)
  val fgR = fgColorRGB(11, 8)
  val fgG = fgColorRGB(7, 4)
  val fgB = fgColorRGB(3, 0)

  val bgR = bgColorRGB(11, 8)
  val bgG = bgColorRGB(7, 4)
  val bgB = bgColorRGB(3, 0)

  val activeArea = io.activeArea

  // --- Select Pixel Source ---
  // Determine if the current pixel is part of the character shape (1) or background (0)
  val pixelVal = io.pixelData(~io.xIndex)

  // --- Multiplex Output Color ---
  // Choose between foreground and background color based on pixelVal
  when(activeArea) {
    io.r := Mux(pixelVal.asBool, fgR, bgR)
    io.g := Mux(pixelVal.asBool, fgG, bgG)
    io.b := Mux(pixelVal.asBool, fgB, bgB)
  }.otherwise({
    io.r := 0.U
    io.g := 0.U
    io.b := 0.U
  })
}

object VgaPixelRenderer extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new VgaPixelRenderer())
}
