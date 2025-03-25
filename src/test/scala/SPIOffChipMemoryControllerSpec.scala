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
      

      dut.clock.step(10);

      val address = 0x112233.U

      dut.interconnectPort.address.poke(address)
      dut.interconnectPort.rd.poke(true.B)

      dut.clock.step(1);
      // chip select should be low after receiving the request
      dut.qspiPort.chipSelect.expect(false.B)
      dut.clock.step(1)

      var previousSpiClk = false

      def isSpiRisingEdge(): Boolean = {
        dut.qspiPort.spiClk.peek().litToBoolean && !previousSpiClk
      }

      val instruction = Array.ofDim[Boolean](8)
      for (i <- 0 to 7 by 1)
      {
        while (!isSpiRisingEdge())
         // Wait until rising edge of spi
         {
          previousSpiClk = dut.qspiPort.spiClk.peek().litToBoolean
          dut.clock.step(1)
          }
        previousSpiClk = dut.qspiPort.spiClk.peek().litToBoolean

        val bit = dut.qspiPort.data0Out.peek().litToBoolean
        instruction.update(i, bit)
        dut.clock.step(1)
      }

      val expectedInstruction = "11101011" // todo: verify this is the actual value we want
      val obtainedInstruction = instruction.map(if (_) "1" else "0").mkString
      assert (expectedInstruction==obtainedInstruction, s"Expected $expectedInstruction but got $obtainedInstruction")



    }
  }
}