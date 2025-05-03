import chisel3._

class NativeMemory2Pipecon(
  DATA_WIDTH: Int = 32, 
  ADDR_WIDTH: Int = 9, 
  WMASK_WIDTH: Int = 4
) extends Module {
  val io = IO(new Bundle {
    val pipe = new PipeCon(32)
    val mem = new NativeMemoryInterface(DATA_WIDTH, ADDR_WIDTH, WMASK_WIDTH)
  })

  val ackreg = RegInit(false.B)
  val enable = io.pipe.rd || io.pipe.wr

  // Chip select, writemask
  io.mem.cs := enable
  io.mem.wmask := io.pipe.wrMask
  io.mem.wen := io.pipe.wr

  //ack
  ackreg := enable
  io.pipe.ack := ackreg

  // Data/Adress
  io.mem.address := io.pipe.address(ADDR_WIDTH-1,0)
  io.pipe.rdData := io.mem.rdata
  io.mem.wdata := io.pipe.wrData
}

object NativeMemory2Pipecon extends App {
  // Use ChiselStage instead of Driver
  (new chisel3.stage.ChiselStage).emitVerilog(new NativeMemory2Pipecon(), Array("--target-dir", "generated"))
}
