import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ConfigIoBFM(port: configIO, clock: Clock) {
  def setupConfigFlash() = {
    port.jedec.poke(false.B)
    port.clear.poke(false.B)
    port.clockDivision.poke(2.U)
    port.mode.poke(false.B)
    port.targetFlash.poke(true.B)
  }

  def setupConfigRAM() = {
    port.jedec.poke(false.B)
    port.clear.poke(false.B)
    port.clockDivision.poke(2.U)
    port.mode.poke(false.B)
    port.targetFlash.poke(false.B)
  }
}

class PipeConIoBFM(port: PipeCon, clock: Clock) {
  def triggerRead(address: Int) = {
    port.address.poke(address.U)
    port.rd.poke(true.B)
  }

  def triggerWrite(address: Int, data: UInt) = {
    port.address.poke(address.U)
    port.wrData.poke(data)
    port.wr.poke(true.B)
    port.rd.poke(false.B)
    port.wrMask.poke("b1111".U)
  }

  def waitUntilAck() = {
    while (!port.ack.peek().litToBoolean) {
      clock.step(1)
    }
  }

  def expectData(data: BigInt) = {
    val obtained = port.rdData.peek().litValue
    assert(
      data == obtained,
      s"[expectDataOut] Expected $data, got $obtained"
    )
  }

}

class SpiIoBFM(port: spiIO, clock: Clock) {
  var previousSpiClk = false

  def isSpiRisingEdge(): Boolean = {
    port.spiClk.peek().litToBoolean && !previousSpiClk
  }

  def updatePrevSpiClk() = {
    previousSpiClk = port.spiClk.peek().litToBoolean
  }

  def expectChipEnable(value: Boolean) {
    val obtained = port.chipSelect.peekBoolean()
    assert(obtained == value, s"[ChipEnable] Expected $value, got $obtained")
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

  def expectFunctionCode(funcCode: BigInt) = {
    val obtainedFunctCode = receiveFunctionCode()
    assert(
      obtainedFunctCode == funcCode,
      s"[FunctionCode] Expected $funcCode, got $obtainedFunctCode"
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

      val bit = port.dataOut.peek().litToBoolean
      obtainedAddress.update(i, bit)
      // println(f"Bit $i: $bit, Address so far: b${obtainedAddress.map(if (_) "1" else "0").mkString}")

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
      s"[Address] Expected $address, got $obtainedAddress"
    )
  }

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

  def expectData(data: BigInt) = {
    val obtainedData = receiveData()
    // we're expecting to receive bits in reverse order!!!
    val reversedData = reverseData(obtainedData)
    assert(
      data == reversedData,
      s"[Data] Expected $data, got $reversedData"
    )
  }

  def reverseData(data: BigInt): BigInt = {
    var result = BigInt(0)
    for (i <- 0 until 32) {
      if ((data.testBit(i))) {
        result = result.setBit(32 - 1 - i)
      }
    }
    result
  }

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

  def stepClock(times: Int = 1) = {
    for (i <- 0 until times) {
      while (!isSpiRisingEdge())
        // Wait until rising edge of spi
        {
          updatePrevSpiClk()
          clock.step()
        }

      updatePrevSpiClk()
      clock.step()
    }
  }
}

class BridgeSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Bridge"
  it should "Handle a read instruction for the Flash" in {
    test(
      new Bridge()
    ) { dut =>
      val config = new ConfigIoBFM(dut.config, dut.clock)
      val interconnect = new PipeConIoBFM(dut.pipeCon, dut.clock)
      val spi = new SpiIoBFM(dut.spiPort, dut.clock)

      // 1. config
      config.setupConfigFlash()

      // 2. insert data on PipeCon
      val address = 0x112233
      interconnect.triggerRead(address)
      dut.clock.step(2)

      // 3. verify what comes out in spiPort
      spi.expectChipEnable(false)

      spi.expectFunctionCode(FlashInstructions.readDataInstruction.litValue)

      spi.expectAddress(address)

      val response = "hdeadbeef".U(32.W)
      val responseBits = response.asBools

      // simulate response from the off chip memory
      spi.pokeData(responseBits)

      // verify data returned through the interconnect
      interconnect.waitUntilAck()
      interconnect.expectData(response.litValue)
      spi.expectChipEnable(true)

    }
  }
  it should "Handle a write instruction for the Flash" in {
    test(
      new Bridge()
    ) { dut =>
      val config = new ConfigIoBFM(dut.config, dut.clock)
      val interconnect = new PipeConIoBFM(dut.pipeCon, dut.clock)
      val spi = new SpiIoBFM(dut.spiPort, dut.clock)

      // 1. config
      config.setupConfigFlash()

      // 2. insert data on PipeCon
      val address = 0x112234
      val data = "hdeadbeef".U(32.W)
      interconnect.triggerWrite(address, data)

      // 3. verify what comes out in spiPort
      spi.expectFunctionCode(FlashInstructions.writeEnableInstruction.litValue)
      spi.expectFunctionCode(FlashInstructions.pageProgramInstruction.litValue)

      // FlashInstructions.readStatusRegister1Instruction

      spi.expectAddress(address)
      spi.expectData(data.litValue)

      // to check the status of the operation (if the mem is busy or not..)
      spi.expectFunctionCode(
        FlashInstructions.readStatusRegister1Instruction.litValue
      )

      interconnect.waitUntilAck()
      spi.expectChipEnable(true)

    }

  }
//   it should "Handle a read instruction for the RAM" in {
//     test(
//       new Bridge()
//     ) { dut =>
//       val config = new ConfigIoBFM(dut.config, dut.clock)
//       val interconnect = new PipeConIoBFM(dut.pipeCon, dut.clock)
//       val spi = new SpiIoBFM(dut.spiPort, dut.clock)

//       // 1. config
//       config.setupConfigRAM()

//       // 2. insert data on PipeCon
//       val address = 0xf42135
//       interconnect.triggerRead(address)
//       dut.clock.step(2)

//       // 3. verify what comes out in spiPort
//       spi.expectChipEnable(false)

//       spi.expectFunctionCode(RAMInstructions.readInstruction.litValue)

//       spi.expectAddress(address)

//       val response = "hdeadbeef".U(32.W)
//       val responseBits = response.asBools

//       // simulate response from the off chip memory
//       spi.pokeData(responseBits)

//       // verify data returned through the interconnect
//       interconnect.waitUntilAck()
//       interconnect.expectData(response.litValue)
//       spi.expectChipEnable(true)

//     }

//   }
//   it should "Handle a write instruction for the RAM" in {
//     test(
//       new Bridge()
//     ) { dut =>
//       val config = new ConfigIoBFM(dut.config, dut.clock)
//       val interconnect = new PipeConIoBFM(dut.pipeCon, dut.clock)
//       val spi = new SpiIoBFM(dut.spiPort, dut.clock)

//       // 1. config
//       config.setupConfigRAM()

//       // 2. insert data on PipeCon
//       val address = 0x112234
//       val data = "hdeadbeef".U(32.W)
//       interconnect.triggerWrite(address, data)

//       // 3. verify what comes out in spiPort
//       spi.expectFunctionCode(FlashInstructions.writeEnableInstruction.litValue)
//       spi.expectFunctionCode(FlashInstructions.pageProgramInstruction.litValue)

//       // FlashInstructions.readStatusRegister1Instruction

//       spi.expectAddress(address)
//       spi.expectData(data.litValue)

//       // to check the status of the operation (if the mem is busy or not..)
//       spi.expectFunctionCode(
//         FlashInstructions.readStatusRegister1Instruction.litValue
//       )

//       interconnect.waitUntilAck()
//       spi.expectChipEnable(true)

//     }

//   }
}
