import chisel3._

class MemoryInterface16KB(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val pipe = new PipeCon(addrWidth)
    val memaddress = Output(UInt(12.W))
    val memwdata = Output(UInt(32.W))
    val memrdata = Input(UInt(32.W))
    val memcs = Output(Bool())
    val memwen = Output(UInt(4.W))     
  })

  val ackreg = RegInit(false.B)
  val enable = io.pipe.rd || io.pipe.wr

  // Chip select, writemask
  io.memcs := enable
  io.memwen := io.pipe.wrMask

  //ack
  ackreg := enable
  io.pipe.ack := ackreg

  // Data/Adress
  io.memaddress := io.pipe.address(11,0)
  io.pipe.rdData := io.memrdata
  io.memwdata := io.pipe.wrData
}

object MemoryInterface16KB extends App {
  // Use ChiselStage instead of Driver
  (new chisel3.stage.ChiselStage).emitVerilog(new MemoryInterface16KB(32), Array("--target-dir", "generated"))  //Address width must be at least 12
}
