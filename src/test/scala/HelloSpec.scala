import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class HelloSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Hello"
  it should "do something" in {
    test(new Hello) { dut =>
      dut.clock.step(1)
      dut.out.expect(42.U)
    }
  }
}