import chisel3._
import chisel3.util._
import chisel.lib.dclib.DCAsyncFifo // Needed by VgaTop
import VgaConfig._ // Import VGA constants

class VgaPeripheral(
    val pipeConAddrWidth: Int = 32,
    val fifoDepth: Int = 8
) extends Module {

  val io = IO(new Bundle {
    val bus = Flipped(new PipeCon(pipeConAddrWidth))

    // Direct VGA outputs
    val hSync = Output(Bool())
    val vSync = Output(Bool())
    val r = Output(UInt(4.W))
    val g = Output(UInt(4.W))
    val b = Output(UInt(4.W))
  })

  val vgaTop = Module(new VgaTop(fifoDepth))

  vgaTop.io.write.valid := io.bus.wr

  vgaTop.io.write.bits.addr := io.bus.address(CHAR_BUFFER_ADDR_WIDTH - 1, 0)

  // Map PipeCon write data (lower 16 bits) to FIFO data bits
  vgaTop.io.write.bits.data := io.bus.wrData(15, 0)

  val ackReg = RegInit(false.B)

  val shouldAck = (io.bus.wr && vgaTop.io.write.ready) || io.bus.rd

  ackReg := shouldAck
  io.bus.ack := ackReg

  // There's nothing to read from the VGA controller soo we just set it to 0
  io.bus.rdData := 0.U(32.W)

  // --- Connect to VGA physical pins ---
  io.hSync := vgaTop.io.hSync
  io.vSync := vgaTop.io.vSync
  io.r := vgaTop.io.r
  io.g := vgaTop.io.g
  io.b := vgaTop.io.b
}

// Object to generate Verilog for the VgaPeripheral
object VgaPeripheral extends App {
  (new chisel3.stage.ChiselStage)
    .emitVerilog(
      new VgaPeripheral(pipeConAddrWidth = 32, fifoDepth = 8)
    )
}
