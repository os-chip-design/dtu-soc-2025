import chisel3._
import wildcat.pipeline.ThreeCats

class PipeConInterconnect(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val cpu = new PipeCon(addrWidth)  // CPU interface
    val uart = Flipped(new PipeCon(addrWidth))  // UART interface
  })

  // Instantiate the modules
  val cpu = Module(new ThreeCats())  // CPU module
  val uart = Module(new UARTPeripheral(addrWidth))  // UART peripheral

  // Default assigments
  io.uart.wrMask(0) := false.B
  io.uart.wrMask(1) := false.B
  io.uart.wrMask(2) := false.B
  io.uart.wrMask(3) := false.B
  io.uart.address := 0.U
  io.uart.rd := false.B
  io.uart.wr := false.B
  io.uart.wrData := 0.U

  uart.io.rd := false.B
  uart.io.wr := false.B
  uart.io.wrMask(0) := false.B
  uart.io.wrMask(1) := false.B
  uart.io.wrMask(2) := false.B
  uart.io.wrMask(3) := false.B
  uart.io.address := 0.U
  uart.io.wrData := 0.U

  cpu.io.imem.stall := false.B
  cpu.io.imem.data := 0.U
  cpu.io.dmem.stall := false.B
  cpu.io.dmem.rdData := 0.U

  io.cpu.ack := false.B
  io.cpu.rdData := 0.U

  // Address decode: check if CPU wants to access UART (address 0x01)
  val uartAddress = 0x01.U(addrWidth.W)

  // CPU read and write logic
  when(io.cpu.address === uartAddress) {
    
    // Handle read/write logic to UART
    when(io.cpu.rd) {
      // Read from UART
      io.uart.rd := true.B
      io.uart.wr := false.B
      io.cpu.rdData := io.uart.rdData  // Pass the UART read data to CPU
    }.elsewhen(io.cpu.wr) {
      // Write to UART
      io.uart.rd := false.B
      io.uart.wr := true.B
      io.uart.wrData := io.cpu.wrData  // Pass CPU write data to UART
    }
  }.otherwise {
    // No access to UART, so CPU does not interact with UART
    io.cpu.ack := false.B
    io.uart.rd := false.B
    io.uart.wr := false.B
  }

  // Other address spaces could be handled here for different peripherals
}