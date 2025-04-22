import chisel3._

class UARTPeripheral(addrWidth: Int) extends Module {
  val io = IO(new PipeCon(addrWidth))  // Use PipeCon as the interface
  
  // Simulate UART behavior for this example
  // This is a simple behavior where we store data on write and return it on read.
  val uartMemory = RegInit(0.U(32.W))  // 32-bit register for UART data

  io.ack := false.B
  
  // If the write signal is asserted, store the write data in uartMemory
  when(io.wr) {
    uartMemory := io.wrData
  }

  // If the read signal is asserted, return the stored data
  io.rdData := Mux(io.rd, uartMemory, 0.U)  // Return stored data on read
}
