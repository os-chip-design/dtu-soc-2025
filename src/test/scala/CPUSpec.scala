import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CPUSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Hello"
  it should "do something" in {
    test(new CPU) { dut =>
      for (i <- 0 until 10) {
        print(f"${dut.out_address.peekInt()}%s ${dut.out_instr.peekInt()}%s ${dut.out_wrData.peekInt()}%s")
        println(f" ${dut.out_decodedInstr}%s")
        dut.clock.step(1)
      }

      // Does not currently work.
      // dut.out.expect(42.U)
    }
  }
}