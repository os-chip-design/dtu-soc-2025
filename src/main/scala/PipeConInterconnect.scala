import chisel3._

class PipeConInterconnect(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val cpu = (new PipeCon(addrWidth))  // From CPU
    val uart = Flipped(new PipeCon(addrWidth))          // To UART
  })

  // Defaults
  io.uart.rd := false.B
  io.uart.wr := false.B
  io.uart.address := 0.U
  io.uart.wrData := 0.U
  io.uart.wrMask := VecInit(Seq.fill(4)(false.B))

  io.cpu.ack := false.B
  io.cpu.rdData := 0.U

  val uartAddress = 0x01.U(addrWidth.W)
  // Debugging: Print the CPU address to the console
  printf(p"CPU address:  ${io.cpu.address}\n")
  printf(p"UART address: ${uartAddress}\n")
  printf(p"CPU rd: ${io.cpu.rd}\n")
  printf(p"CPU wr: ${io.cpu.wr}\n")
  when(io.cpu.address === uartAddress) {
    when(io.cpu.rd) {
      io.uart.rd := true.B
      io.uart.wr := false.B
      io.uart.address := io.cpu.address
      io.cpu.rdData := io.uart.rdData
      io.cpu.ack := io.uart.ack
    }.elsewhen(io.cpu.wr) {
      io.uart.wr := true.B
      io.uart.rd := false.B
      io.uart.address := io.cpu.address
      io.uart.wrData := io.cpu.wrData
      io.uart.wrMask := io.cpu.wrMask
      io.cpu.ack := io.uart.ack
    }
  }
}
