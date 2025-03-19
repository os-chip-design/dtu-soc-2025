import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CPUSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Hello"
  it should "store 42 from a immediate" in {
    test(new CPU(
      RISCVCompiler.inlineASM(
        """.text
addi x1, x0, 42
sw x1, 0(x0)""")
    ))(dut => {
      dut.clock.step(3)
      dut.debugMem_rdAddress.poke(0)
      dut.clock.step()
      dut.debugMem_rdData.expect(42)
    })
  }

  it should "store 42 at 0x69 from memory" in {
    test(new CPU(
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
      dut.clock.step(4)
      dut.debugMem_rdAddress.poke(0x69)
      dut.clock.step()
      dut.debugMem_rdData.expect(42)
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

    test(new CPU(p))(dut => {
      dut.clock.step(6)
      dut.debugMem_rdAddress.poke(4)
      dut.clock.step()
      dut.debugMem_rdData.expect(42)
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

    test(new CPU(p))(dut => {
      dut.clock.step(11)
      dut.debugMem_rdAddress.poke(16)
      dut.clock.step()
      // dut.debugMem_rdData.expect(42)
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
      """
    )

    test(new CPU(p))(dut => {
      dut.clock.step(11)
      dut.debugMem_rdAddress.poke(16)
      dut.clock.step()
      // dut.debugMem_rdData.expect(42)
    })
  }

  it should "store 42 at 0x128 from memory with C" in {
    val p = RISCVCompiler.inlineC(
      """
      int main() {
        volatile int *ptr = (int *) 0x10;
        *ptr = 42;
        asm("jal 4000");
      }""")

    test(new CPU(p))(dut => {
      dut.clock.step(30)
      dut.debugMem_rdAddress.poke(0x10)
      dut.clock.step()
      // dut.debugMem_rdData.expect(42)
    })
  }
}
