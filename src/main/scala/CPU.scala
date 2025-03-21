import chisel3._
import chisel3.util._
import wildcat.pipeline.Functions.decode
import wildcat.pipeline._

class FakeCPUInstr(program: RISCVCompiler.CompiledProgram) extends Module {
  val io = IO(Flipped(new InstrIO()))
  val idxReg = RegInit(0.U(32.W))

  val instructions = VecInit(program._1.toIndexedSeq.map(_.S(32.W).asUInt))

  idxReg := io.address / 4.U

  when(idxReg >= instructions.length.U) {
    io.data := 0x00000013.U
  }.otherwise {
    io.data := instructions(idxReg)
  }

  printf(p"$idxReg | ${io.data} | ${decode(io.data)}\n")

  io.stall := false.B
}

class FakeCPUMem(program: RISCVCompiler.CompiledProgram, size: Int = 4096) extends Module {
  val io = IO(Flipped(new MemIO()))
  val mem = SyncReadMem(size, UInt(32.W), SyncReadMem.WriteFirst)

  // Assert that the program fits in memory
  assert(program._1.length <= size)

  val mems = Array(
    SyncReadMem(size / 4, UInt(8.W), SyncReadMem.WriteFirst),
    SyncReadMem(size / 4, UInt(8.W), SyncReadMem.WriteFirst),
    SyncReadMem(size / 4, UInt(8.W), SyncReadMem.WriteFirst),
    SyncReadMem(size / 4, UInt(8.W), SyncReadMem.WriteFirst))

  // split an integer into a seq of 4 bytes
  // little endian, so first byte in seq goes to mem(0)
  // Split program._2 into a list of 4 length lists, then zip inner lists into four lists
  program._2.map(x => (0 until 4).map(i => (x >> (i * 8)) & 0xff).toArray).transpose.zipWithIndex.map(x => {
    val (data, i) = x
    data.zipWithIndex.map(y => {
      val (byte, j) = y
      mems(i).write(j.U, byte.U)
    })
  })

  val truncatedRdAddress = io.rdAddress(31, 2)
  val truncatedWrAddress = io.wrAddress(31, 2)

  io.rdData := mems.reverse.map(_.read(truncatedRdAddress)).reduce(_ ## _)
  printf(p"Read ${io.rdData} from ${truncatedRdAddress}\n")

  for (i <- 0 until 4) {
    when(io.wrEnable(i)) {
      printf(p"Writing ${io.wrData(8 * i + 7, 8 * i)} to ${truncatedWrAddress}\n")
      mems(i).write(truncatedWrAddress, io.wrData(8 * i + 7, 8 * i))
    }
  }

  io.stall := false.B
}

class CPU(program: RISCVCompiler.CompiledProgram) extends Module {
  println(".text")
  program._1.foreach(x => println(f"${x}%08x"))
  println(".data")
  program._2.foreach(x => println(f"${x}%08x"))

  val cpu = Module(new ThreeCats())
  val instr = Module(new FakeCPUInstr(program))
  val mem = Module(new FakeCPUMem(program))

  cpu.io.dmem <> mem.io
  instr.io.address := cpu.io.imem.address
  cpu.io.imem.data := instr.io.data
  cpu.io.imem.stall := instr.io.stall

  val debugMem_rdAddress = IO(Input(UInt(32.W)))
  val debugMem_rdData = IO(Output(UInt(32.W)))

  mem.io.rdAddress := debugMem_rdAddress
  debugMem_rdData := mem.io.rdData
}

object CPU extends App {
  emitVerilog(new CPU(RISCVCompiler.emptyProgram), Array("--target-dir", "generated"))
}
