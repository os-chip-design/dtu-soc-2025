
import chisel3._
import chisel3.util._
import wildcat.pipeline.ThreeCats
import wildcat.pipeline.ScratchPadMem

class PipeConInterconnect(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val cpu = (new PipeCon(addrWidth))  // From CPU
    val uart = Flipped(new PipeCon(addrWidth))          // To UART
    val SPI = Flipped(new PipeCon(addrWidth))
  })

  //val addrmem = Array(1,2,3)
  //val dmem = Module(new ScratchPadMem(addrmem))
  //val cpu2 = Module(new ThreeCats())
  //cpu2.io.dmem <> dmem.io


  // Defaults
  io.uart.rd := false.B
  io.uart.wr := false.B
  io.uart.address := 0.U
  io.uart.wrData := 0.U
  io.uart.wrMask := "b0000".U

  io.SPI.rd := false.B
  io.SPI.wr := false.B
  io.SPI.address := 0.U
  io.SPI.wrData := 0.U
  io.SPI.wrMask := "b0000".U

  io.cpu.ack := false.B
  io.cpu.rdData := 0.U

  val uartAddress = 0x01.U(addrWidth.W)
  val SPIAddress = 0x02.U(addrWidth.W)
  // Debugging: Print the CPU address to the console
  //printf(p"CPU address:  ${io.cpu.address}\n")
  //printf(p"UART address: ${uartAddress}\n")
  //printf(p"CPU rd: ${io.cpu.rd}\n")
  //printf(p"CPU wr: ${io.cpu.wr}\n")
  when(io.cpu.address === uartAddress) {
    when(io.cpu.rd) {
      io.uart.rd := true.B
      io.uart.wr := false.B
      io.uart.address := io.cpu.address
      io.cpu.rdData := io.uart.rdData
      io.cpu.ack := io.uart.ack
    }.elsewhen(io.cpu.wrMask.orR) {
      io.uart.rd := false.B
      io.uart.wr := true.B
      io.uart.address := io.cpu.address
      io.uart.wrData := io.cpu.wrData
      io.uart.wrMask := io.cpu.wrMask
      io.cpu.ack := io.uart.ack
    }
  }.elsewhen(io.cpu.address === SPIAddress) {
    when(io.cpu.rd) {
      io.SPI.rd := true.B
      io.SPI.wr := false.B
      io.SPI.address := io.cpu.address
      io.cpu.rdData := io.SPI.rdData
      io.cpu.ack := io.SPI.ack
    }.elsewhen(io.cpu.wrMask.orR) {
      io.SPI.rd := false.B
      io.SPI.wr := true.B
      io.SPI.address := io.cpu.address
      io.SPI.wrData := io.cpu.wrData
      io.SPI.wrMask := io.cpu.wrMask
      io.cpu.ack := io.SPI.ack
    }
  }
}
