import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipeComBFM(port: PipeCon, clock: Clock) {
  def triggerRead(address: Int) = {
    port.address.poke(address)
    port.rd.poke(true.B)
  }
}

class QspiIoBFM(port: qspiIO, clock: Clock) {
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

      val bit = port.data0Out.peek().litToBoolean
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
      s"Expected $funcCode, got $obtainedFunctCode"
    )
  }

  def receiveAddress(): Int = {
    // next step in the SPI is providing the 24-bit address
    val obtainedAddress = Array.ofDim[Boolean](24)
    for (i <- 0 to 23 by 1) {
      while (!isSpiRisingEdge())
        // Wait until rising edge of spi
        {
          updatePrevSpiClk()
          clock.step()
        }

      val bit = port.data0Out.peek().litToBoolean
      obtainedAddress.update(i, bit)

      updatePrevSpiClk()
      clock.step()
    }

    val obtainedAddressStr = obtainedAddress.map(if (_) "1" else "0").mkString
    val obtainedAddressInt: Int = Integer.parseInt(obtainedAddressStr, 2)

    obtainedAddressInt
  }

  def expectAddress(address: Int) = {
    val obtainedAddress = receiveAddress()
    assert(
      address == obtainedAddress,
      s"Expected $address, got $obtainedAddress"
    )
  }
}

class SPIOffChipMemoryControllerSpec
    extends AnyFlatSpec
    with ChiselScalatestTester {
  behavior of "SPIOffChipMemoryController"
  it should "Read a 32-bit value correctly" in {
    test(
      new SPIOffChipMemoryControllerWrapper(
        addrWidth = 24,
        dataWidth = 32,
        spiFreq = 2,
        freq = 10,
        configuredIntoQSPI = true
      )
    ) { dut =>
      dut.clock.step(10);
      val interconnect = new PipeComBFM(dut.interconnectPort, dut.clock)
      val qspi = new QspiIoBFM(dut.qspiPort, dut.clock)

      val address = 0x112233
      interconnect.triggerRead(address)
      dut.clock.step(1);

      // chip select should be false
      qspi.expectChipEnable(false)
      dut.clock.step(1)

      // delete these two
      var previousSpiClk = false
      def isSpiRisingEdge(): Boolean = {
        dut.qspiPort.spiClk.peek().litToBoolean && !previousSpiClk
      }

      val expectedInstruction: Int = Integer.parseInt("01101011", 2)
      qspi.expectFunctionCode(expectedInstruction)
      dut.clock.step()

      qspi.expectAddress(address)

      // dummy data that should  be used by the controller
      dut.qspiPort.data0In.poke(true.B)
      dut.qspiPort.data1In.poke(false.B)
      dut.qspiPort.data2In.poke(true.B)
      dut.qspiPort.data3In.poke(true.B)

      // now comes the dummy clock cycles
      for (i <- 0 to 7 by 1) {
        while (!isSpiRisingEdge())
          // Wait until rising edge of spi
          {
            previousSpiClk = dut.qspiPort.spiClk.peek().litToBoolean
            dut.clock.step(1)
          }
        previousSpiClk = dut.qspiPort.spiClk.peek().litToBoolean
        dut.clock.step(1)
      }

      val charData = "a"

      /*
        for (i <- 0 until 2)
        {
          while (!isSpiRisingEdge())
          // Wait until rising edge of spi
          {
            previousSpiClk = dut.qspiPort.spiClk.peek().litToBoolean
            dut.clock.step(1)
          }

          var shift = 0;

          if (i % 2 == 0)
          {
            shift = 4;
          }
          val charIndex = i/2;
          dut.qspiPort.data3In.poke((charArray(charIndex).asDigit & (0x8 << shift)) != 0)
          dut.qspiPort.data2In.poke((charArray(charIndex).asDigit & (0x4 << shift)) != 0)
          dut.qspiPort.data1In.poke((charArray(charIndex).asDigit & (0x2 << shift)) != 0)
          dut.qspiPort.data0In.poke((charArray(charIndex).asDigit & (0x1 << shift)) != 0)


          previousSpiClk = dut.qspiPort.spiClk.peek().litToBoolean
          dut.clock.step(1)
        }
       */

      dut.clock.step(200)

      // this is not finished

      val hexString = f"0x${dut.interconnectPort.rdData.peek().litValue}%X"

    }
  }

  it should "Handle SPIO clk correctly" in {
    test(
      new SPIOffChipMemoryControllerWrapper(
        addrWidth = 24,
        dataWidth = 32,
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
        dut.qspiPort.spiClk.peek().litToBoolean == false,
        "spiClk should be false"
      )
      assert(spiClkmax > 0, "spiClkmax should be greater than 0")
      dut.clock.step(spiClkmax)
      assert(
        dut.qspiPort.spiClk.peek().litToBoolean == true,
        "spiClk should be true"
      )
      dut.clock.step(spiClkmax - 1)
      assert(
        dut.qspiPort.spiClk.peek().litToBoolean == true,
        "spiClk should still be true"
      )
      dut.clock.step(1)
      assert(
        dut.qspiPort.spiClk.peek().litToBoolean == false,
        "spiClk should be false"
      )
    }
  }
}
