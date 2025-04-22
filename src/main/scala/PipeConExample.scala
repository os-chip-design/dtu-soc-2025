import chisel3._

class PipeConExample(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val cpuAddress = Input(UInt(addrWidth.W))
    val cpuWrData = Input(UInt(32.W))
    val cpuRd = Input(Bool())
    val cpuWr = Input(Bool())
    val cpuRdData = Output(UInt(32.W))

    val uartRdDataTest = Input(UInt(32.W))  // Test data input for UART read simulation
    val uartRdData = Output(UInt(32.W))
    val uartWr = Output(Bool())
    val uartRd = Output(Bool())
    val uartWrData = Output(UInt(32.W))
  })

  val interconnect = Module(new PipeConInterconnect(addrWidth))
  val uart = Module(new UARTPeripheral(addrWidth))

  // Drive the CPU-side of the interconnect
  interconnect.io.cpu.address := io.cpuAddress
  interconnect.io.cpu.rd := io.cpuRd
  interconnect.io.cpu.wr := io.cpuWr
  interconnect.io.cpu.wrData := io.cpuWrData
  interconnect.io.cpu.wrMask := VecInit(Seq.fill(4)(true.B)) // full-word write

  io.cpuRdData := interconnect.io.cpu.rdData

  // Connect interconnect to UART
  uart.io <> interconnect.io.uart

  // Connect uartRdDataTest to testRdData for simulation
  uart.testIo.testRdData := io.uartRdDataTest  // Drive the test data to the UART peripheral

  // Expose UART signals
  io.uartRdData := uart.io.rdData  // UART read data from the peripheral
  io.uartWr := uart.io.wr
  io.uartRd := uart.io.rd
  io.uartWrData := uart.io.wrData
}
