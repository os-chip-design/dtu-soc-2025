import chisel3._

class PipeConExample(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val cpuAddress = Input(UInt(addrWidth.W))
    val cpuWrData = Input(UInt(32.W))
    val cpuRd = Input(Bool())
    val cpuWrMask = Input(Vec (4, Bool()))
    val cpuRdData = Output(UInt(32.W))

    val uartRdDataTest = Input(UInt(32.W))  // Test data input for UART read simulation
    val uartRdData = Output(UInt(32.W))
    val uartRd = Output(Bool())
    val uartWrMask = Output(Vec (4, Bool()))
    val uartWrData = Output(UInt(32.W))

    val SPIRdDataTest = Input(UInt(32.W))  // Test data input for UART read simulation
    val SPIRdData = Output(UInt(32.W))
    val SPIRd = Output(Bool())
    val SPIWrMask = Output(Vec (4, Bool()))
    val SPIWrData = Output(UInt(32.W))
  })

  val interconnect = Module(new PipeConInterconnect(addrWidth))
  val uart = Module(new UARTPeripheral(addrWidth))
  val SPI = Module(new SPIPeripheral(addrWidth))

  // Drive the CPU-side of the interconnect
  interconnect.io.cpu.address := io.cpuAddress
  interconnect.io.cpu.rd := io.cpuRd
  interconnect.io.cpu.wrData := io.cpuWrData
  interconnect.io.cpu.wrMask := io.cpuWrMask //VecInit(Seq.fill(4)(true.B)) // full-word write

  io.cpuRdData := interconnect.io.cpu.rdData

  // Connect interconnect to UART
  uart.io <> interconnect.io.uart
  SPI.io <> interconnect.io.SPI

  // Connect uartRdDataTest to testRdData for simulation
  uart.testIo.testRdData := io.uartRdDataTest  // Drive the test data to the UART peripheral
  SPI.testIo.testRdData := io.SPIRdDataTest

  // Expose UART signals
  io.uartRdData := uart.io.rdData  // UART read data from the peripheral
  io.uartRd := uart.io.rd
  io.uartWrMask := uart.io.wrMask
  io.uartWrData := uart.io.wrData

  io.SPIRdData := SPI.io.rdData  // SPI read data from the peripheral
  io.SPIRd := SPI.io.rd
  io.SPIWrMask := SPI.io.wrMask
  io.SPIWrData := SPI.io.wrData
}
