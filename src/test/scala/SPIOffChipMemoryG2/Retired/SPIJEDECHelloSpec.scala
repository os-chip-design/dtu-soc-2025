import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class SpiIoBFM_jedec(port: spiIO, clock: Clock) {
  var previousSpiClk = false

  def isSpiRisingEdge(): Boolean = {
    port.spiClk.peek().litToBoolean && !previousSpiClk
  }

  def updatePrevSpiClk() = {
    previousSpiClk = port.spiClk.peek().litToBoolean
  }

  def expectChipEnable(value: Boolean) {
    val obtained = port.chipSelect.peekBoolean()
    assert(obtained == value, s"Expected $value, got $obtained")
  }

  def receiveFunctionCode(): Int = {
    val instruction = Array.ofDim[Boolean](8)
    for (i <- 0 until 8) {
      // Wait until rising edge of spi
      while (!isSpiRisingEdge()) {
        updatePrevSpiClk()
        clock.step(1)
      }

      val bit = port.dataOut.peek().litToBoolean
      instruction.update(i, bit)

      updatePrevSpiClk()
      clock.step(1)
    }
    val obtainedInstructionStr = instruction.map(if (_) "1" else "0").mkString
    val obtainedInstructionInt: Int =
      Integer.parseInt(obtainedInstructionStr, 2)

    obtainedInstructionInt
  }

  def expectFunctionCode(funcCode: Int) = {
    val obtainedFunctCode = receiveFunctionCode()
    assert(
      obtainedFunctCode == funcCode,
      s"[expectFunctionCode] Expected $funcCode, got $obtainedFunctCode"
    )
  }

  def pokeData(data: Seq[chisel3.Bool]) = {
    for (i <- 0 to 23 by 1) {
      while (!isSpiRisingEdge())
        // Wait until rising edge of spi
        {
          updatePrevSpiClk()
          clock.step()
        }

      port.dataIn.poke(data(i))

      updatePrevSpiClk()
      clock.step()
    }
  }

}

class SPIJEDECHelloSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SPIOffChipMemoryController"
  it should "Read the JEDEC of the memory" in {
    test(
      new SPIJEDECHello(
        spiFreq = 2,
        freq = 10
      )
    ) { dut =>
      // dut.clock.step()

      val spi = new SpiIoBFM_jedec(dut.spiPort, dut.clock)
      dut.interconnectPort.rd.poke(true.B)
      // chip select should be false
      spi.expectChipEnable(false)
      dut.clock.step(1)

      val expectedInstruction: Int = Integer.parseInt("10011111", 2)
      spi.expectFunctionCode(expectedInstruction)
      dut.clock.step()

      val response = "b111101111010111001011001".U(24.W)
      val responseBits = response.asBools

      spi.pokeData(responseBits)

      dut.clock.step()
      dut.clock.step(200)
      // dut.JEDECout.dataOut.expect(response)
      val obtained = dut.interconnectPort.rdData.peek()
      assert(
        response.litValue == obtained.litValue,
        s"[expectDataOut] Expected $response, got $obtained"
      )

    }
  }

  it should "Handle SPIO clk correctly" in {
    test(
      new SPIJEDECHello(
        spiFreq = 1000000,
        freq = 50000000
      )
    ) { dut =>
      // test the spiIOCclk
      dut.clock.step(10)
      dut.reset.poke(true.B)
      dut.clock.step(1)
      dut.reset.poke(false.B)
      val spiClkmax = dut.freq / dut.spiFreq / 2
      assume(
        dut.spiPort.spiClk.peek().litToBoolean == false,
        "spiClk should be false"
      )
      assert(spiClkmax > 0, "spiClkmax should be greater than 0")
      dut.clock.step(spiClkmax)
      assert(
        dut.spiPort.spiClk.peek().litToBoolean == true,
        "spiClk should be true"
      )
      dut.clock.step(spiClkmax - 1)
      assert(
        dut.spiPort.spiClk.peek().litToBoolean == true,
        "spiClk should still be true"
      )
      dut.clock.step(1)
      assert(
        dut.spiPort.spiClk.peek().litToBoolean == false,
        "spiClk should be false"
      )
    }
  }
}
