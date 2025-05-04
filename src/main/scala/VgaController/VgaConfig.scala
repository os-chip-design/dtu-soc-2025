import chisel3.util.log2Ceil

object VgaConfig {
  // VGA 640x480 @ 60Hz Timing Constants (VESA Standard)
  val H_DISPLAY = 640
  val H_FP = 16
  val H_SYNC_PULSE = 96
  val H_BP = 48
  val H_TOTAL = H_DISPLAY + H_FP + H_SYNC_PULSE + H_BP

  val V_DISPLAY = 480
  val V_FP = 10
  val V_SYNC_PULSE = 2
  val V_BP = 33
  val V_TOTAL = V_DISPLAY + V_FP + V_SYNC_PULSE + V_BP

  val H_SYNC_START = H_DISPLAY + H_FP
  val H_SYNC_END = H_SYNC_START + H_SYNC_PULSE
  val V_SYNC_START = V_DISPLAY + V_FP
  val V_SYNC_END = V_SYNC_START + V_SYNC_PULSE

  // Bit widths derived from constants
  val H_TOTAL_BITS = log2Ceil(H_TOTAL)
  val V_TOTAL_BITS = log2Ceil(V_TOTAL)
  val H_DISPLAY_BITS = log2Ceil(H_DISPLAY)
  val V_DISPLAY_BITS = log2Ceil(V_DISPLAY)

  // Character Grid & Indexing Related Constants
  val CHAR_WIDTH_PIXELS = 8
  val CHAR_HEIGHT_PIXELS = 16
  val CHAR_COLS = H_DISPLAY / CHAR_WIDTH_PIXELS
  val CHAR_ROWS = V_DISPLAY / CHAR_HEIGHT_PIXELS
  val CHAR_X_INDEX_BITS = log2Ceil(CHAR_WIDTH_PIXELS)
  val CHAR_Y_INDEX_BITS = log2Ceil(CHAR_HEIGHT_PIXELS)
  val H_COORD_CHAR_START_BIT = CHAR_X_INDEX_BITS
  val H_COORD_CHAR_END_BIT = H_DISPLAY_BITS - 1
  val V_COORD_CHAR_START_BIT = CHAR_Y_INDEX_BITS
  val V_COORD_CHAR_END_BIT = V_DISPLAY_BITS - 1
  val CHAR_BUFFER_ADDR_WIDTH = log2Ceil(CHAR_COLS * CHAR_ROWS)
}
