import chisel3._
import chisel3.util.experimental.BoringUtils
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import wildcat.pipeline.{InstrIO, InstructionROM, MemIO, ScratchPadMem, ThreeCats}

class NOPInstrMemory(data: Array[Int]) extends Module {
  val io = IO(Flipped(new InstrIO()))
  val idxReg = RegInit(0.U(32.W))

  val instructions = VecInit(data.toIndexedSeq.map(_.S(32.W).asUInt))

  idxReg := io.address / 4.U

  when(idxReg >= instructions.length.U) {
    io.data := 0x00000013.U
  }.otherwise {
    io.data := instructions(idxReg)
  }

  io.stall := false.B
}


class SingleCPUMem(data: Array[Int], size: Int = 4096) extends Module {
  val io = IO(Flipped(new MemIO()))
  val mem = SyncReadMem(
    size,
    UInt(32.W),
    SyncReadMem.WriteFirst
  )

  // Assert that the program fits in memory
  assert(data.length <= size)

  data.zipWithIndex.foreach(x => {
    mem.write(x._2.U, x._1.U)
  })

  private val truncatedRdAddress = io.rdAddress
  private val truncatedWrAddress = io.wrAddress

  io.rdData := mem.read(truncatedRdAddress)

  printf(p"[rd] addr: $truncatedRdAddress | data: ${io.rdData}\n")

  // Read the current 32-bit word from memory
  private val oldData = mem.read(truncatedWrAddress)

  // Compute the new data by modifying only enabled bytes.
  // Here we use a fold that updates the accumulated value (acc) for each byte.
  private val newData = (0 until 4).foldLeft(oldData) { (acc, i) =>
    val shift = i * 8
    // Create a mask for the 8 bits to update: 0xff shifted to the correct position.
    val byteMask = (0xff.U(32.W) << shift)
    // Extract the new byte from wrData.
    val newByte = io.wrData((i + 1) * 8 - 1, i * 8) << shift
    // If the write-enable for this byte is high, update; otherwise, keep the accumulated value.
    Mux(io.wrEnable(i), (acc & (~byteMask).asUInt) | newByte.asUInt, acc)
  }

  // Write the modified word back to memory
  mem.write(truncatedWrAddress, newData)

  // when any wrEnable printf newData
  when(io.wrEnable.reduce(_ || _)) {
    printf(p"[wr] addr: $truncatedWrAddress | input: ${io.wrData} | newData: ${newData}\n")
  }

  io.stall := false.B
}

class CPUTestTop(program: RISCVCompiler.CompiledProgram) extends Module {
  println(".text")
  program._1.foreach(x => println(f"${x}%08x"))
  println(".data")
  program._2.foreach(x => println(f"${x}%08x"))

  val nrBytes = 4096

  val debugMemory = IO(Flipped(new MemIO()))
  val enableDebugMemory = IO(Input(Bool()))

  // Here you can switch between different CPU designs:
  val cpu = Module(new ThreeCats())
  // val cpu = Module(new WildFour())
  // val cpu = Module(new StandardFive())
  val dmem = Module(new ScratchPadMem(program._2, nrBytes))
  val imem = Module(new InstructionROM(program._1))

  cpu.io.imem <> imem.io

  cpu.io.dmem.rdData := dmem.io.rdData
  cpu.io.dmem.stall := dmem.io.stall

  debugMemory.rdData := dmem.io.rdData
  debugMemory.stall := dmem.io.stall

  dmem.io.rdAddress := Mux(enableDebugMemory, debugMemory.rdAddress, cpu.io.dmem.rdAddress)
  dmem.io.rdEnable := Mux(enableDebugMemory, debugMemory.rdEnable, cpu.io.dmem.rdEnable)
  dmem.io.wrAddress := Mux(enableDebugMemory, debugMemory.wrAddress, cpu.io.dmem.wrAddress)
  dmem.io.wrData := Mux(enableDebugMemory, debugMemory.wrData, cpu.io.dmem.wrData)
  dmem.io.wrEnable := Mux(enableDebugMemory, debugMemory.wrEnable, cpu.io.dmem.wrEnable)

  val debugRegs = IO(Output(Vec(32, UInt(32.W))))
  val debugCpuStop = IO(Output(Bool()))

  debugRegs := DontCare
  debugCpuStop := DontCare

  BoringUtils.bore(cpu.debugRegs, Seq(debugRegs))
  BoringUtils.bore(cpu.stop, Seq(debugCpuStop))
}


class CPUSpec extends AnyFlatSpec with ChiselScalatestTester {
  def expectMemory(dut: CPUTestTop, addr: Int, v: Int): Unit = {
    dut.enableDebugMemory.poke(true.B)
    dut.debugMemory.rdEnable.poke(true.B)
    dut.debugMemory.rdAddress.poke(addr.U)
    dut.clock.step()
    dut.debugMemory.rdData.expect(v.U)
    dut.enableDebugMemory.poke(false.B)
  }

  behavior of "CPU"
  it should "store 42 from a immediate" in {
    test(new CPUTestTop(
      RISCVCompiler.inlineASM(
        """
addi x1, x0, 42
sw x1, 0(x0)
""")
    ))(dut => {
      dut.clock.step(3)
      expectMemory(dut, 0, 42)
    })
  }

  it should "store 42 at 0x69 from memory" in {
    test(new CPUTestTop(
      RISCVCompiler.inlineASM(
        """
.data
data:
  .word 42
.text
  la x1, data
  lw x2, 0(x1)
  sw x2, 0x69(x0)
""")))(dut => {
      dut.clock.step(6)
      expectMemory(dut, 0x69, 42)
    })
  }

  it should "not remove my register value to write it to memory" in {
    val p = RISCVCompiler.inlineASM(
      """
      li a5, 4      # Memory address to write data
      li a4, 69     # Data to write
      sw a4, 0(a5)  # Write data to memory
      li a4, 42     # Data to write 
      sw a4, 0(a5)  # Write data to memory
      """
    )

    test(new CPUTestTop(p))(dut => {
      dut.clock.step(6)
      expectMemory(dut, 4, 42)
    })
  }

  it should "work with assembly as well as with C (full)" in {
    val p = RISCVCompiler.inlineASM(
      """
      li   sp,256
      addi sp,sp,-32
      sw   ra,28(sp)
      sw   s0,24(sp)
      addi s0,sp,32
      li   a5,16
      sw   a5,-20(s0)
      lw   a5,-20(s0)
      li   a4,42
      sw   a4,0(a5)
      """
    )

    test(new CPUTestTop(p))(dut => {
      dut.clock.step(20)
      expectMemory(dut, 16, 42)
    })
  }

  it should "work with assembly as well as with C (deconstructed)" in {
    val p = RISCVCompiler.inlineASM(
      """
    li   a5,16
    sw   a5,59(x0)
    lw   a5,59(x0)
    li   a4,42
    sw   a4,0(a5)

    li t1, 16
    bne a5, t1, fail
    
    li t1, 42
    lw t2, 0(a5)
    bne t1, t2, fail

pass:
  addi a0, x0, 0
	ecall

fail:
  addi a0, x0, 2
	ecall
      """
    )

    test(new CPUTestTop(p))(dut => {

      while (!dut.debugCpuStop.peek().litToBoolean) {
        dut.clock.step()
      }

      dut.debugRegs(10).expect(0.U)
      expectMemory(dut, 16, 42)
    })
  }

  it should "store 42 at 0x128 from memory with C" in {
    val p = RISCVCompiler.inlineC(
      """
      int main() {
        volatile int *ptr = (int *) 0x10;
        *ptr = 42;
        asm("ecall");
      }""")

    test(new CPUTestTop(p))(dut => {
      while (!dut.debugCpuStop.peek().litToBoolean) {
        dut.clock.step()
      }

      expectMemory(dut, 0x10, 42)
    })
  }
}
