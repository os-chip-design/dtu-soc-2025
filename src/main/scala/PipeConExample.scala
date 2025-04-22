import chisel3._

class PipeConExample(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    // Exposed CPU-side signals for testing
    val cpuAddress = Input(UInt(addrWidth.W))
    val cpuWrData = Input(UInt(32.W))
    val cpuRd = Input(Bool())
    val cpuWr = Input(Bool())
    val cpuRdData = Output(UInt(32.W))

    // For debugging or observing UART state
    val uartWr = Output(Bool())
    val uartRd = Output(Bool())
    val uartRdData = Output(UInt(32.W))
    val uartWrData = Output(UInt(32.W))
  })

  // Instantiate the interconnect (with embedded CPU and UART)
  val interconnect = Module(new PipeConInterconnect(addrWidth))

  // Default assignments
  interconnect.io.uart.rdData := 0.U
  interconnect.io.uart.ack := false.B

  // Drive the interconnect from testbench
  interconnect.io.cpu.address := io.cpuAddress
  interconnect.io.cpu.rd := io.cpuRd
  interconnect.io.cpu.wr := io.cpuWr
  interconnect.io.cpu.wrData := io.cpuWrData
  interconnect.io.cpu.wrMask := VecInit(Seq.fill(4)(true.B)) // assume full-word write

  // Expose CPU read data
  io.cpuRdData := interconnect.io.cpu.rdData

  // Optionally expose UART signals for checking
  io.uartWr := interconnect.io.uart.wr
  io.uartRd := interconnect.io.uart.rd
  io.uartRdData := interconnect.io.uart.rdData
  io.uartWrData := interconnect.io.uart.wrData
}
