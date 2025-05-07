import chisel3._

class NativeMemory2Pipecon(
  DATA_WIDTH: Int = 32, 
  ADDR_WIDTH: Int = 9, 
  WMASK_WIDTH: Int = 4
) extends Module {
  val io = IO(new Bundle {
    val pipe = new PipeCon(32)
    val native = new NativeMemoryInterface(DATA_WIDTH, ADDR_WIDTH, WMASK_WIDTH)
  })

  val ackreg = RegInit(false.B)
  val enable = io.pipe.rd || io.pipe.wr

  // Chip select, writemask
  io.native.cs := ~enable // native.cs is active low
  io.native.wmask := io.pipe.wrMask
  io.native.wen := ~io.pipe.wr // native.wen is active low

  //ack
  ackreg := enable
  io.pipe.ack := ackreg

  // Data/Adress
  io.native.address := io.pipe.address(ADDR_WIDTH-1,0)
  io.pipe.rdData := io.native.rdata
  io.native.wdata := io.pipe.wrData
}

object NativeMemory2Pipecon extends App {
  // Use ChiselStage instead of Driver
  (new chisel3.stage.ChiselStage).emitVerilog(new NativeMemory2Pipecon(), Array("--target-dir", "generated"))
}
