import chisel3._
import wildcat.pipeline._

class PipeConInterconnect(addrWidth: Int, addressRanges: Seq[(UInt, UInt)]) extends Module {
  val io = IO(new Bundle {
    val device = Vec(addressRanges.length, Flipped(new PipeCon(addrWidth))) // Create a vector of devices
    // Input from the CPU
    val dmem = Flipped(new MemIO())
  })

  for (i <- 0 until io.device.length) {
    io.device(i).rd := false.B
    io.device(i).wr := false.B
    io.device(i).address := 0.U
    io.device(i).wrData := 0.U
    io.device(i).wrMask := 0.U
  }

  val rdDataReg = RegInit(0.U(32.W))
  val stall = RegInit(false.B)

  val selected = Wire(new PipeCon(addrWidth))

  selected.rd := false.B
  selected.wr := false.B
  selected.address := 0.U
  selected.wrData := 0.U
  selected.wrMask := 0.U
  selected.rdData := 0.U
  selected.ack := false.B

  // Select device to read/write from/to based on addressRanges
  for (i <- 0 until io.device.length) {
    val (startAddr, endAddr) = addressRanges(i)
    when(io.dmem.wrAddress >= startAddr && io.dmem.wrAddress <= endAddr) {
      selected <> io.device(i)
    }
  }

  // Logic for reading/writing from/to devices
  when(io.dmem.wrEnable.reduce(_ || _)) {
    selected.wr := true.B
    selected.rd := false.B
    selected.address := io.dmem.wrAddress
    selected.wrData := io.dmem.wrData
    selected.wrMask := io.dmem.wrEnable.asUInt
  }.elsewhen(io.dmem.rdEnable) {
    selected.rd := true.B
    selected.wr := false.B
    selected.address := io.dmem.rdAddress
    rdDataReg := selected.rdData
  }

  io.dmem.stall := stall
  io.dmem.rdData := rdDataReg
}
