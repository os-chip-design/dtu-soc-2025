import chisel3._
import chisel3.util._

class UARTPeripheral(addrWidth: Int) extends Module {
  val io = IO(new PipeCon(addrWidth))  // Use PipeCon as the interface
  
  // Additional interface for testing
  val testIo = IO(new Bundle {
    val testWrData = Input(UInt(32.W)) // Only used for testing
  })

  // Register for UART data (simulating a simple UART with a single 32-bit register)
  val uartMemory = RegInit(0.U(32.W))  // 32-bit register to store UART data
  
  // Acknowledge signal (ack to signal the completion of the transaction)
  io.ack := false.B

  // Expand each Bool to a full 8-bit mask
  val byteMasks = (0 until 4).map { i =>
    Mux(io.wrMask(i), "hFF".U(8.W), 0.U(8.W))
  }
  // Concatenate to get a full 32-bit mask (MSB to LSB)
  val fullMask = Cat(byteMasks.reverse)  // Vec is LSB-first, Cat expects MSB-first

  // If a write operation is requested, store the data in uartMemory
  when(io.wrMask.orR) {
    uartMemory := io.wrData & fullMask
    //printf("[UART_P] Uart write: Data written = 0x%x, Mask = 0x%x, Memory = 0x%x\n", io.wrData, fullMask, uartMemory)
    io.ack := true.B  // Acknowledge that the write is complete
  }

  //printf("[UART_P] Full Mask: 0x%x\n", fullMask)


  // If a read operation is requested, provide the stored data or test data
  when(io.rd) {
    //printf("UART read: Addr = 0x%x, Data = 0x%x\n", io.address, uartMemory)
    // During testing, use the testRdData input for read data
    io.rdData := Mux(testIo.testWrData === 0.U, uartMemory, testIo.testWrData)  // If testRdData is set, use it
    io.ack := true.B  // Acknowledge that the read is complete
  }.otherwise {
    io.rdData := 0.U  // If not reading, return 0 by default
  }
}
