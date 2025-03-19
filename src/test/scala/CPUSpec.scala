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
}
