import chisel3._
import chisel3.util._
import chisel.lib.dclib.DCAsyncFifo
import VgaConfig._

/** Top-level wrapper for the VgaController with clock divider and Asyncronous
  * write to the character buffer This is the one that should be used in the
  * final design!
  *
  * Assumes the implicit system clock is 100 MHz. Includes a clock divider
  * (100MHz -> 25MHz), manual reset synchronization (2-flop), and uses
  * DCAsyncFifo for handling writes (address + data) to the character buffer
  * across clock domains. The documentation around multible clocks in chisel is
  * not stellar, but this seems to work from the testing i've managed to do
  *
  * @param fifoDepth
  *   Depth of the asynchronous FIFO. Must be a power of 2.
  */
class VgaTop(
    val fifoDepth: Int = 8
) extends Module {

  // Ensure fifoDepth is a power of 2 for DCAsyncFifo
  require(isPow2(fifoDepth), "fifoDepth must be a power of 2 for DCAsyncFifo")

  // internal Bundle for data passing through the FIFO
  class FifoData extends Bundle {
    val addr = UInt(CHAR_BUFFER_ADDR_WIDTH.W)
    val data = UInt(16.W) // Matches VgaController.io.wrData width
  }

  val io = IO(new Bundle {
    // --- System Clock Domain Inputs ---
    val write = Flipped(Decoupled(new FifoData)) // Use internal bundle

    // --- VGA Clock Domain (Generated 25 MHz) Outputs ---
    val vgaClkOut = Output(Clock())
    val hSync = Output(Bool())
    val vSync = Output(Bool())
    val r = Output(UInt(4.W))
    val g = Output(UInt(4.W))
    val b = Output(UInt(4.W))
  })

  // Hardcode clock division ratio for 100MHz -> 25MHz. Can be changed in case we want to use a different clock speed
  val clockDivRatio = 4
  val clkDivCounterWidth = log2Ceil(clockDivRatio)

  // --- Clock Generation ---
  // This should probably be extracted to a seperate module
  val vgaClkReg = RegInit(false.B)
  val clkDivCounter = RegInit(0.U(clkDivCounterWidth.W))
  clkDivCounter := clkDivCounter + 1.U
  when(clkDivCounter === (clockDivRatio / 2 - 1).U) {
    vgaClkReg := !vgaClkReg
  }
  when(clkDivCounter === (clockDivRatio - 1).U) {
    clkDivCounter := 0.U
  }
  val vgaClock = vgaClkReg.asClock
  io.vgaClkOut := vgaClock

  // --- Manual Reset Synchronization (2-Flop) ---
  val sysResetBool = reset.asBool
  val resetFlop1 = withClock(vgaClock) { RegNext(sysResetBool, true.B) }
  val resetFlop2 = withClock(vgaClock) { RegNext(resetFlop1, true.B) }
  val vgaResetSyncBool = resetFlop2
  val vgaReset = vgaResetSyncBool.asAsyncReset

  // --- Asynchronous FIFO (DCAsyncFifo) ---
  // Instantiate DCAsyncFifo using the internal FifoData bundle
  val writeFifo = Module(
    new DCAsyncFifo(
      data = new FifoData,
      depth = fifoDepth
    )
  )

  // Connect clocks  to the FIFO
  writeFifo.io.enqClock := clock
  writeFifo.io.enqReset := reset
  writeFifo.io.deqClock := vgaClock
  writeFifo.io.deqReset := vgaReset

  // Connect external write interface (CPU) to FIFO enqueue
  writeFifo.io.enq <> io.write

  // --- Instantiate VgaController (VGA Clock Domain) ---
  val vgaController = withClockAndReset(vgaClock, vgaReset) {
    Module(new VgaController())
  }

  // --- Connect FIFO Dequeue to VgaController Write Port (VGA Clock Domain) ---
  withClockAndReset(vgaClock, vgaReset) {
    // Connect dequeued data fields to VgaController's separate inputs
    vgaController.io.wrAddr := writeFifo.io.deq.bits.addr
    vgaController.io.wrData := writeFifo.io.deq.bits.data
    vgaController.io.wrEnable := writeFifo.io.deq.valid

    // Signal FIFO ready based on its valid output
    writeFifo.io.deq.ready := writeFifo.io.deq.valid
  }

  // --- Connect VgaController Outputs (VGA Clock Domain) ---
  io.hSync := vgaController.io.hSync
  io.vSync := vgaController.io.vSync
  io.r := vgaController.io.r
  io.g := vgaController.io.g
  io.b := vgaController.io.b

}

// Object to generate Verilog
object VgaTop extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new VgaTop(fifoDepth = 8))
}
