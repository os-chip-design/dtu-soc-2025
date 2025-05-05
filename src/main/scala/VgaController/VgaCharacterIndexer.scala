import chisel3._
import chisel3.util._

import VgaConfig._

/** IO Bundle for the VgaCharacterIndexer. Takes pixel coordinates and active
  * status from VgaTimer. Outputs character indices and the prefetched base
  * address for the Character Buffer.
  */
class VgaCharacterIndexerIo extends Bundle {

  // Inputs from timer
  val pixelX = Input(UInt(H_DISPLAY_BITS.W))
  val pixelY = Input(UInt(V_DISPLAY_BITS.W))
  val hActive = Input(Bool())
  val vActive = Input(Bool())

  // Outputs for Character Buffer and Font ROM
  val xCharIndex = Output(
    UInt(CHAR_X_INDEX_BITS.W)
  ) // X offset within character glyph
  val yCharIndex = Output(
    UInt(CHAR_Y_INDEX_BITS.W)
  ) // Y offset within character glyph
  val charBaseAddr = Output(
    UInt(CHAR_BUFFER_ADDR_WIDTH.W)
  ) // Base address for Character Buffer (prefetched)
}

/** Calculates character grid indices and prefetches the character buffer
  * address. Takes pixel coordinates and active status from VgaTimer. Designed
  * to provide the address needed for the *next* clock cycle's read from a
  * synchronous character buffer RAM.
  */
class VgaCharacterIndexer extends Module {
  val io = IO(new VgaCharacterIndexerIo)

  // --- Character Index Calculation (within glyph) ---
  val currentXIndex = io.pixelX(CHAR_X_INDEX_BITS - 1, 0)
  val currentYIndex = io.pixelY(CHAR_Y_INDEX_BITS - 1, 0)

  // --- Character Grid Position Calculation ---
  val currentHCharGridPos =
    io.pixelX(H_COORD_CHAR_END_BIT, H_COORD_CHAR_START_BIT)
  val currentVCharGridPos =
    io.pixelY(V_COORD_CHAR_END_BIT, V_COORD_CHAR_START_BIT)

  // --- Prefetch Logic for Character Buffer Address ---
  val isLastPixelInCharX = currentXIndex === (CHAR_WIDTH_PIXELS - 1).U

  val nextHCharGridPos = Mux(
    currentHCharGridPos === (CHAR_COLS - 1).U,
    0.U, // Wrap around if at last column
    currentHCharGridPos + 1.U
  )

  // If it's the last pixel column within the current character glyph, use the *next*
  // character's grid column index for the address. Otherwise, use the current one.
  val prefetchedHCharGridPos = Mux(
    isLastPixelInCharX && io.hActive, // Check if prefetch needed and still horizontally active
    nextHCharGridPos,
    currentHCharGridPos
  )

  // Calculate the final prefetched linear base address
  val calculatedCharBaseAddr =
    (currentVCharGridPos * CHAR_COLS.U) + prefetchedHCharGridPos

  // --- Assign Outputs ---
  when(io.hActive && io.vActive) {
    io.xCharIndex := currentXIndex
    io.yCharIndex := currentYIndex
    io.charBaseAddr := calculatedCharBaseAddr
  }.otherwise {
    // Default outputs for non-active area (blanking intervals)
    io.xCharIndex := 0.U
    io.yCharIndex := 0.U
    val currentVCharGridPos =
      io.pixelY(V_COORD_CHAR_END_BIT, V_COORD_CHAR_START_BIT)
    val startOfLineAddr = (currentVCharGridPos * CHAR_COLS.U)
    io.charBaseAddr := Mux(io.vActive, startOfLineAddr, 0.U)
  }
}

object VgaCharacterIndexer extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new VgaCharacterIndexer())
}
