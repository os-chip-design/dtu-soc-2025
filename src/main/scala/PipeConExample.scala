import wildcat.pipeline.ThreeCats
import chisel3._

class PipeConExample(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val UART   = Flipped(new PipeCon(addrWidth))
//    val SPI    = new PipeCon(addrWidth)
//    val SPImem = new PipeCon(addrWidth)
//    val VGA    = new PipeCon(addrWidth)
//    val KBD    = new PipeCon(addrWidth)
//    val Mem    = new PipeCon(addrWidth)
//    val IOcell = new PipeCon(addrWidth)
  })

  val cpu = Module(new ThreeCats())
  

  cpu.io.imem.stall  := false.B  
  cpu.io.dmem.stall  := false.B  
  cpu.io.imem.data   := "h00000000".U(32.W)  
  cpu.io.dmem.rdData := "h00000000".U(32.W)

  
  val wrDataCPU   = Wire(UInt(32.W))
  val wrMaskCPU   = Wire(Vec (4, Bool()))
  val wrAddrCPU   = Wire(UInt(32.W))  
  val rdAddrCPU   = Wire(UInt(32.W))  
  val rdEnableCPU = WireDefault(false.B)  

  
  
  
  wrDataCPU   := cpu.io.dmem.wrData
  wrMaskCPU   := cpu.io.dmem.wrEnable
  wrAddrCPU   := cpu.io.dmem.wrAddress  
  rdAddrCPU   := cpu.io.dmem.rdAddress  
  rdEnableCPU := cpu.io.dmem.rdEnable  
  

  // Simple memory to simulate a read/write behavior
  val mem = Mem(256, UInt(32.W))  // 256 locations, 32 bits wide

  //io.UART.rdData := 0.U  // Default read data = 0
  //io.UART.ack := false.B // Default acknowledge = false


  

  when(wrMaskCPU.asUInt =/= "b0000".U) {
    io.UART.wr := true.B
    io.UART.wrMask := wrMaskCPU
    for(i <- 0 until addrWidth) {
      if (wrAddrCPU(31-i) == "b1") {
        io.UART.wrData := wrDataCPU
        io.UART.address := wrAddrCPU
        
      }
    }
  }


  //io.UART.rdData := wrDataCPU  // Example read operation
  //io.UART.ack := true.B  // Example write acknowledgment

}

object PipeConExample extends App {
  // Use ChiselStage instead of Driver
  (new chisel3.stage.ChiselStage).emitVerilog(new PipeConExample(8))  // For example, 8-bit address width
}
