import chisel3._
import chisel3.util._

// VGA has some funky timing to be compatible with old scanning
// line monitors. The industry standard that we will use, has
// the following parameters:
// VGA Timing Constants for 640x480 @ 60 Hz (25.175 MHz Pixel Clock)
// Based on VESA standard timing.
// source: http://www.tinyvga.com/vga-timing/640x480@60Hz

// General Timing:
//   Pixel Clock Frequency: 25.175 MHz
//   Screen Refresh Rate:   60 Hz
//   Vertical Refresh:      31.46875 kHz

// Horizontal Timing (pixels):      Total Time: 31.777 us / line
//   Sync Polarity: Negative
// -----------------------------------------------------------------
//   Visible Area (H_DISPLAY):    640 pixels    (25.422 us)
//   Front Porch  (H_FP):          16 pixels    ( 0.636 us)
//   Sync Pulse   (H_SYNC):        96 pixels    ( 3.813 us)
//   Back Porch   (H_BP):          48 pixels    ( 1.907 us)
//   ---------------------------------------------------------------
//   Total Horiz. (H_TOTAL):      800 pixels    (31.778 us)

// Vertical Timing (lines):         Total Time: 16.683 ms / frame
//   Sync Polarity: Negative
// -----------------------------------------------------------------
//   Visible Area (V_DISPLAY):    480 lines     (15.253 ms)
//   Front Porch  (V_FP):           10 lines     ( 0.318 ms)
//   Sync Pulse   (V_SYNC):          2 lines     ( 0.064 ms)
//   Back Porch   (V_BP):           33 lines     ( 1.049 ms)
//   ---------------------------------------------------------------
//   Total Vert.  (V_TOTAL):       525 lines     (16.683 ms)
//

// Notes:
// - HSync is low during the Sync Pulse period (pixels 656 to 751).
// - VSync is low during the Sync Pulse period (lines 490 to 491).
// - hActive is high during Visible Area (pixels 0 to 639).
// - vActive is high during Visible Area (lines 0 to 479).

/** Companion object for VgaTimer to hold configuration constants. (Ensure this
  * object definition is available, either here or elsewhere)
  */

/** Bundle defining the output timing signals for the VGA controller. Pixel
  * coordinates and active signals are now COMBINATIONAL.
  */
import VgaConfig._
class VgaTimerIo extends Bundle {

  // Sync signals (Active Low)
  val hSync = Output(Bool())
  val vSync = Output(Bool())

  // Active area indicators
  val hActive = Output(Bool())
  val vActive = Output(Bool())

  // Pixel coordinates (Cvalid when hActive and vActive are high)
  val pixelX = Output(UInt(H_DISPLAY_BITS.W))
  val pixelY = Output(UInt(V_DISPLAY_BITS.W))

  // Timer enable input
  val timerEn = Input(Bool())
}

/** Generates VGA timing signals for a fixed 640x480 @ 60Hz resolution. Assumes
  * it is clocked by the pixel clock (e.g., 25.175 MHz). Outputs pixel
  * coordinates and active area signals.
  */
class VgaTimer extends Module {
  val io = IO(new VgaTimerIo)

  // --- Counters ---
  val hCounter = RegInit(0.U(H_TOTAL_BITS.W))
  val vCounter = RegInit(0.U(V_TOTAL_BITS.W))

  // --- Counter Logic ---
  val hLast = hCounter === (H_TOTAL - 1).U
  val vLast = vCounter === (V_TOTAL - 1).U

  // Reset vertical counter at the end of frame, and horizontal counter at the end of every line
  when(io.timerEn) {
    hCounter := Mux(hLast, 0.U, hCounter + 1.U)
    when(hLast) {
      vCounter := Mux(vLast, 0.U, vCounter + 1.U)
    }
  }

  io.hSync := !(hCounter >= H_SYNC_START.U && hCounter < H_SYNC_END.U)
  io.vSync := !(vCounter >= V_SYNC_START.U && vCounter < V_SYNC_END.U)
  io.hActive := hCounter < H_DISPLAY.U
  io.vActive := vCounter < V_DISPLAY.U
  io.pixelX := hCounter
  io.pixelY := vCounter

}

object VgaTimer extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new VgaTimer())
}
