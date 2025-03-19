import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CPUSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Hello"
  it should "do something" in {
    test(new CPU(
      RISCVCompiler.inlineASM(
        """.text
addi x1, x0, 42
sw x1, 0(x0)""")
    ))(dut => {
      for (i <- 0 until 10) {
        dut.clock.step()
      }

      dut.debugMem_rdAddress.poke(0)
      dut.clock.step()
      dut.debugMem_rdData.expect(42)
    })
  }
}
