import chisel3._

class UARTPeripheral(addrWidth: Int) extends Module {
  val io = IO(new PipeCon(addrWidth))  // Use PipeCon as the interface
  
  // Additional interface for testing
  val testIo = IO(new Bundle {
    val testRdData = Input(UInt(32.W)) // Only used for testing
  })

  // Register for UART data (simulating a simple UART with a single 32-bit register)
  val uartMemory = RegInit(0.U(32.W))  // 32-bit register to store UART data
  
  // Acknowledge signal (ack to signal the completion of the transaction)
  io.ack := false.B

  // If a write operation is requested, store the data in uartMemory
  when(io.wr) {
    uartMemory := io.wrData
    io.ack := true.B  // Acknowledge that the write is complete
  }

  // If a read operation is requested, provide the stored data or test data
  when(io.rd) {
    // During testing, use the testRdData input for read data
    io.rdData := Mux(testIo.testRdData === 0.U, uartMemory, testIo.testRdData)  // If testRdData is set, use it
    io.ack := true.B  // Acknowledge that the read is complete
  }.otherwise {
    io.rdData := 0.U  // If not reading, return 0 by default
  }
}
