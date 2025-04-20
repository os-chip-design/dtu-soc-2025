import wildcat.pipeline.ThreeCats
import chisel3._

class PipeConExample(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val UART   = (new PipeCon(addrWidth))
    //val SPI    = (new PipeCon(addrWidth))
    //val SPImem = (new PipeCon(addrWidth))
    //val VGA    = (new PipeCon(addrWidth))
    //val KBD    = (new PipeCon(addrWidth))
    //val Mem    = (new PipeCon(addrWidth))
    //val IOcell = (new PipeCon(addrWidth))
  })

  // Default assignments for all PipeCon interfaces
for (iface <- Seq(io.UART)) {//, io.SPI, io.SPImem, io.VGA, io.KBD, io.Mem, io.IOcell)) {
  //iface.ack    := false.B
  //iface.rdData := 0.U
  iface.wrData := 0.U
  iface.wrMask := VecInit(Seq(false.B, false.B, false.B, false.B))
  iface.address := 0.U
  iface.rd      := false.B
  iface.wr      := false.B
}

  val cpu = Module(new ThreeCats())
  
  cpu.io.imem.stall  := false.B  
  cpu.io.dmem.stall  := false.B  
  cpu.io.imem.data   := "h00000000".U(32.W)  
  cpu.io.dmem.rdData := "h00000000".U(32.W)

  val wrDataCPU   = IO(UInt(32.W))
  val wrMaskCPU   = IO(Vec(4, Bool()))
  val wrAddrCPU   = IO(UInt(32.W))  
  val rdAddrCPU   = IO(UInt(32.W))  

  val rdEnableCPU = WireDefault(false.B)  

  wrDataCPU   := cpu.io.dmem.wrData
  wrMaskCPU   := cpu.io.dmem.wrEnable
  wrAddrCPU   := cpu.io.dmem.wrAddress  
  rdAddrCPU   := cpu.io.dmem.rdAddress  
  rdEnableCPU := cpu.io.dmem.rdEnable  

  // Simple memory to simulate a read/write behavior
  val mem = Mem(256, UInt(32.W))  // 256 locations, 32 bits wide

  // Write to UART if the mask is not zero
  when(wrMaskCPU.asUInt =/= "b0000".U) {
    when(wrAddrCPU === 0x00000001.U) {  // Write to UART address
      io.UART.wrData := wrDataCPU
      //io.UART.address := wrAddrCPU  
    }
  }

  // Read from UART
  //when(rdEnableCPU) {
  //  io.UART.rdData := mem(rdAddrCPU)  // Simulate a memory read to UART
  //}
}

object PipeConExample extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new PipeConExample(8))
}
