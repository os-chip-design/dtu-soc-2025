import chisel3._

class PipeConExample(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val pipe = new PipeCon(addrWidth)
  })

  // Simple memory to simulate a read/write behavior
  val mem = Mem(256, UInt(32.W))  // 256 locations, 32 bits wide

  // Intermediate values for ack and read data
  val ack = Wire(Bool())
  val rdData = Wire(UInt(32.W))

  val addr = io.pipe.address
  val data = io.pipe.wrData
  val mask = io.pipe.wrMask

  // Default to zero
  rdData := 0.U
  ack := false.B

  // Read/Write logic
  when(io.pipe.wr) {
    // Write operation
    when(mask(0)) { 
      mem(addr) := data & "h000000FF".U(32.W)
    } 
    when(mask(1)) { 
      mem(addr) := data & "h0000FF00".U(32.W)
    } 
    when(mask(2)) { 
      mem(addr) := data & "h00FF0000".U(32.W)
    } 
    when(mask(3)) { 
      mem(addr) := data & "hFF000000".U(32.W)
    }

    ack := true.B
  }.elsewhen(io.pipe.rd) {
    // Read operation
    rdData := mem(addr)

    ack := true.B
  }.otherwise {
    // No operation, ack remains low
    ack := false.B
  }

  // Assign outputs to the interface
  io.pipe.rdData := rdData
  io.pipe.ack := ack
}

object PipeConExample extends App {
  // Use ChiselStage instead of Driver
  (new chisel3.stage.ChiselStage).emitVerilog(new PipeConExample(8))  // For example, 8-bit address width
}
