import chisel3._
import chisel3.util._


class SPIPeripheral(addrWidth: Int) extends Module {
  val io = IO(new PipeCon(addrWidth))  // Use PipeCon as the interface
  
  // Additional interface for testing
  val testIo = IO(new Bundle {
    val testRdData = Input(UInt(32.W)) // Only used for testing
  })

  // Register for UART data (simulating a simple UART with a single 32-bit register)
  val SPIMemory = RegInit(0.U(32.W))  // 32-bit register to store UART data
  
  // Acknowledge signal (ack to signal the completion of the transaction)
  io.ack := false.B

  // Expand each Bool to a full 8-bit mask
  val byteMasks = io.wrMask.map(b => Mux(b, "hFF".U(8.W), 0.U(8.W)))
  // Concatenate to get a full 32-bit mask (MSB to LSB)
  val fullMask = Cat(byteMasks.reverse)  // Vec is LSB-first, Cat expects MSB-first


  // If a write operation is requested, store the data in uartMemory
  when(io.wrMask.contains(true.B)) {
    SPIMemory := io.wrData & fullMask
    io.ack := true.B  // Acknowledge that the write is complete
  }

  // If a read operation is requested, provide the stored data or test data
  when(io.rd) {
    // During testing, use the testRdData input for read data
    io.rdData := Mux(testIo.testRdData === 0.U, SPIMemory, testIo.testRdData)  // If testRdData is set, use it
    io.ack := true.B  // Acknowledge that the read is complete
  }.otherwise {
    io.rdData := 0.U  // If not reading, return 0 by default
  }
}
