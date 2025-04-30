import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

/** BFM to deal with the configuration inputs.
  *
  * @param port
  *   configuration port
  * @param clock
  *   chip clock
  */
class ConfigIoBFM(port: configIO, clock: Clock) {

  /** Setup configuration to use the Flash.
    */
  def setupConfigFlash() = {
    port.jedec.poke(false.B)
    port.clear.poke(false.B)
    port.clockDivision.poke(2.U)
    port.mode.poke(false.B)
    port.targetFlash.poke(true.B)
  }

  /** Setup configuration to use the RAM.
    */
  def setupConfigRAM() = {
    port.jedec.poke(false.B)
    port.clear.poke(false.B)
    port.clockDivision.poke(2.U)
    port.mode.poke(false.B)
    port.targetFlash.poke(false.B)
  }
}

/** BFM to deal with the PipeCon input and output.
  *
  * @param port
  *   PipeCon port
  * @param clock
  *   chip clock
  */
class PipeConIoBFM(port: PipeCon, clock: Clock) {

  /** Request a read from the address provided.
    *
    * @param address
    *   address from which data should be read
    */
  def triggerRead(address: Int) = {
    port.address.poke(address.U)
    port.rd.poke(true.B)
    port.wr.poke(false.B)
  }

  /** Request a write to the address provided.
    *
    * @param address
    *   address to which data should be written
    * @param data
    *   data to be written
    */
  def triggerWrite(address: Int, data: UInt) = {
    port.address.poke(address.U)
    port.wrData.poke(data)
    port.wr.poke(true.B)
    port.rd.poke(false.B)
    port.wrMask.poke("b1111".U)
  }

  /** Steps the clock until the ack is high.
    */
  def waitUntilAck() = {
    while (!port.ack.peek().litToBoolean) {
      clock.step(1)
    }
  }

  /** Checks if the data received on the PipeCon interface is the same as the
    * provided value.
    *
    * @param data
    *   expected data
    */
  def expectData(data: BigInt) = {
    val obtained = port.rdData.peek().litValue
    assert(
      data == obtained,
      s"[expectDataOut] Expected $data, got $obtained"
    )
  }

}

/** BFM to deal with the SPI interface inputs and outputs.
  *
  * @param port
  *   SPI port
  * @param clock
  *   chip clock
  */
class SpiIoBFM(port: spiIO, clock: Clock) {
  var previousSpiClk = false

  /** Gets the value of the SPI clock.
    *
    * @return
    *   True if the SPI clock has risen in the last chip clock cycle.
    */
  def isSpiRisingEdge(): Boolean = {
    port.spiClk.peek().litToBoolean && !previousSpiClk
  }

  /** Updates a variable keeping track of the last SPI clock state.
    */
  def updatePrevSpiClk() = {
    previousSpiClk = port.spiClk.peek().litToBoolean
  }

  /** Verifies if the Chip Enable pin corresponds to the desired value.
    *
    * @param value
    *   value to compare the pin to
    */
  def expectChipEnable(value: Boolean) {
    val obtained = port.chipSelect.peekBoolean()
    assert(obtained == value, s"[ChipEnable] Expected $value, got $obtained")
  }

  /** Obtains the function code from the SPI interface.
    *
    * @return
    *   8 bits received
    */
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

  /** Verify that the function code received corresponds to the expected value.
    *
    * @param funcCode
    *   expected value of the function code
    */
  def expectFunctionCode(funcCode: BigInt) = {
    val obtainedFunctCode = receiveFunctionCode()
    assert(
      obtainedFunctCode == funcCode,
      s"[FunctionCode] Expected $funcCode, got $obtainedFunctCode"
    )
  }

  /** Obtains the address from the SPI interface.
    *
    * @return
    *   24 bits received
    */
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

      val bit = port.dataOut.peek().litToBoolean
      obtainedAddress.update(i, bit)

      updatePrevSpiClk()
      clock.step()
    }

    val obtainedAddressStr = obtainedAddress.map(if (_) "1" else "0").mkString
    val obtainedAddressInt: Int = Integer.parseInt(obtainedAddressStr, 2)

    obtainedAddressInt
  }

  /** Verify that the address received corresponds to the expected value.
    *
    * @param address
    *   expected value of the address
    */
  def expectAddress(address: Int) = {
    val obtainedAddress = receiveAddress()
    assert(
      address == obtainedAddress,
      s"[Address] Expected $address, got $obtainedAddress"
    )
  }

  /** Obtains the read data from the SPI interface.
    *
    * @return
    *   32 bits received
    */
  def receiveData(): BigInt = {
    // next step in the SPI is providing the 24-bit address
    val obtainedData = Array.ofDim[Boolean](32)
    for (i <- 0 to 31 by 1) {
      while (!isSpiRisingEdge())
        // Wait until rising edge of spi
        {
          updatePrevSpiClk()
          clock.step()
        }

      val bit = port.dataOut.peek().litToBoolean
      obtainedData.update(i, bit)
      updatePrevSpiClk()
      clock.step()
    }

    val obtainedDataStr = obtainedData.map(if (_) "1" else "0").mkString
    val obtainedDataInt: BigInt = BigInt(obtainedDataStr, 2)
    obtainedDataInt
  }

  /** Verify that the read data received corresponds to the expected value.
    *
    * @param data
    *   expected value of the read data
    */
  def expectData(data: BigInt) = {
    val obtainedData = receiveData()
    // we're expecting to receive bits in reverse order!!!
    val reversedData = reverseData(obtainedData)
    assert(
      data == reversedData,
      s"[Data] Expected $data, got $reversedData"
    )
  }

  /** Reverses a 32 bit value.
    *
    * @param data
    *   data to be inverted
    *
    * @return
    *   data reversed (considering a 32 bit value)
    */
  def reverseData(data: BigInt): BigInt = {
    var result = BigInt(0)
    for (i <- 0 until 32) {
      if ((data.testBit(i))) {
        result = result.setBit(32 - 1 - i)
      }
    }
    result
  }

  /** Input data to the SPI interface (simulating what the off chip memory would
    * do).
    *
    * @param data
    *   data to be sent
    */
  def pokeData(data: Seq[chisel3.Bool]) = {
    for (i <- 0 to 31 by 1) {
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

class BridgeSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Bridge"
  it should "Handle a read instruction for the Flash" in {
    test(
      new BridgeWrapper()
    ) { dut =>
      // create the BFMs to deal with input
      val config = new ConfigIoBFM(dut.config, dut.clock)
      val interconnect = new PipeConIoBFM(dut.pipeCon, dut.clock)
      val spi = new SpiIoBFM(dut.spiPort, dut.clock)

      // configure the component to use the Flash
      config.setupConfigFlash()

      // trigger a read through the PipeCon interface
      val address = 0x112233
      interconnect.triggerRead(address)
      dut.clock.step(2)

      // verify that CE is low
      //spi.expectChipEnable(false)

      // verify what comes out in spiPort
      spi.expectFunctionCode(Instructions.readDataInstruction.litValue)
      spi.expectAddress(address)

      val response = "hdeadbeef".U(32.W)
      val responseBits = response.asBools

      // simulate response from the off chip memory
      spi.pokeData(responseBits)

      // verify data returned through the interconnect
      interconnect.waitUntilAck()
      interconnect.expectData(response.litValue)

      // verify that CE is high to show that the operation is finished
      spi.expectChipEnable(true)

    }
  }
  it should "Handle a write instruction for the Flash" in {
    test(
      new BridgeWrapper()
    ) { dut =>
      // create the BFMs to deal with input
      val config = new ConfigIoBFM(dut.config, dut.clock)
      val interconnect = new PipeConIoBFM(dut.pipeCon, dut.clock)
      val spi = new SpiIoBFM(dut.spiPort, dut.clock)

      // configure the component to use the Flash
      config.setupConfigFlash()

      // trigger a write through the PipeCon interface
      val address = 0x112234
      val data = "hdeadbeef".U(32.W)
      interconnect.triggerWrite(address, data)

      // verify what comes out in spiPort
      spi.expectFunctionCode(Instructions.writeEnableInstruction.litValue)
      spi.expectFunctionCode(Instructions.pageProgramInstruction.litValue)

      // verify what comes out through the SPI interface
      spi.expectAddress(address)
      spi.expectData(data.litValue)

      // to check the status of the operation (if the mem is busy or not..)
      spi.expectFunctionCode(
        Instructions.readStatusRegister1Instruction.litValue
      )

      // verify that ack is activated and that CE goes high
      // showing the the operation is complete
      interconnect.waitUntilAck()
      spi.expectChipEnable(true)

    }

  }
  it should "Handle a read instruction for the RAM" in {
    test(
      new BridgeWrapper()
    ) { dut =>
      // create the BFMs to deal with input
      val config = new ConfigIoBFM(dut.config, dut.clock)
      val interconnect = new PipeConIoBFM(dut.pipeCon, dut.clock)
      val spi = new SpiIoBFM(dut.spiPort, dut.clock)

      // configure the component to use the RAM
      config.setupConfigRAM()

      // trigger a read through the PipeCon interface
      val address = 0xf42135
      interconnect.triggerRead(address)
      dut.clock.step(2)

      // verify what comes out in spiPort
      //spi.expectChipEnable(false)
      spi.expectFunctionCode(Instructions.readDataInstruction.litValue)
      spi.expectAddress(address)

      val response = "hdeadbeef".U(32.W)
      val responseBits = response.asBools

      // simulate response from the off chip memory
      spi.pokeData(responseBits)

      // verify data returned through the interconnect
      interconnect.waitUntilAck()
      interconnect.expectData(response.litValue)

      // verify that the communication is terminated
      spi.expectChipEnable(true)

    }

  }
  it should "Handle a write instruction for the RAM" in {
    test(
      new BridgeWrapper()
    ) { dut =>
      // create the BFMs to deal with input
      val config = new ConfigIoBFM(dut.config, dut.clock)
      val interconnect = new PipeConIoBFM(dut.pipeCon, dut.clock)
      val spi = new SpiIoBFM(dut.spiPort, dut.clock)

      // configure the component to use the RAM
      config.setupConfigRAM()

      // trigger a write through the PipeCon interface
      val address = 0x112234
      val data = "hdeadbeef".U(32.W)
      interconnect.triggerWrite(address, data)

      // verify what comes out in spiPort
      spi.expectFunctionCode(Instructions.pageProgramInstruction.litValue)
      spi.expectAddress(address)
      spi.expectData(data.litValue)

      // verify that an ack comes through the PipeCon interface
      interconnect.waitUntilAck()

      // verify that the operation is terminated by pulling CE high
      spi.expectChipEnable(true)

    }

  }
}
