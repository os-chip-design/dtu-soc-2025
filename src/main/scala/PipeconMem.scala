import wildcat.pipeline._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

/**
 * On-chip memory with one clock cycle read timing and write forwarding
 */
class PipeConMem(data: Array[Int], nrBytes: Int = 4096) extends Module {
  val io = IO(Flipped(new MemIO()))

  val mems = Array(
    SyncReadMem(nrBytes/4, UInt(8.W), SyncReadMem.WriteFirst),
    SyncReadMem(nrBytes/4, UInt(8.W), SyncReadMem.WriteFirst),
    SyncReadMem(nrBytes/4, UInt(8.W), SyncReadMem.WriteFirst),
    SyncReadMem(nrBytes/4, UInt(8.W), SyncReadMem.WriteFirst))

  /* not used, would be too easy
  val dataHex = data.map(_.toHexString).mkString("\n")
  val file = new java.io.PrintWriter("data.hex")
  file.write(dataHex)
  file.close()
  loadMemoryFromFile(mem, "data.hex")
   */
  val idx = log2Up(nrBytes/4)
  io.rdData := mems(3).read(io.rdAddress(idx+2, 2)) ## mems(2).read(io.rdAddress(idx+2, 2)) ## mems(1).read(io.rdAddress(idx+2, 2)) ## mems(0).read(io.rdAddress(idx+2, 2))
  when(io.wrEnable(0)) {
    mems(0).write(io.wrAddress(idx+2, 2), io.wrData(7, 0))
  }
  when(io.wrEnable(1)) {
    mems(1).write(io.wrAddress(idx+2, 2), io.wrData(15, 8))
  }
  when(io.wrEnable(2)) {
    mems(2).write(io.wrAddress(idx+2, 2), io.wrData(23, 16))
  }
  when(io.wrEnable(3)) {
    mems(3).write(io.wrAddress(idx+2, 2), io.wrData(31, 24))
  }
  io.stall := false.B
}
