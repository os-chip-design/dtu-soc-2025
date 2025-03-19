import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class SPIOffChipMemoryControllerSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SPIOffChipMemoryController"
  it should "Read a 32-bit value correctly" in {
    test(new SPIOffChipMemoryController(
      addrWidth = 24,
      dataWidth = 32,
      spiFreq = 2,
      freq = 10
    )) { dut =>
      // TODO
    }
  }
}