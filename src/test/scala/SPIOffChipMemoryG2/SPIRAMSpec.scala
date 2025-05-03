import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class pipeConWriter(port: PipeCon, clock: Clock) {
  def triggerWrite(address: UInt, data: UInt) = {
    port.address.poke(address)
    port.wr.poke(true.B)
    port.wrData.poke(data)
    port.wrMask.poke("hf".U(4.W))
  }
  def triggerRead(address: UInt) = {
    port.address.poke(address)
    port.rd.poke(true.B)
  }
  def stopRead() = {
    port.rd.poke(false.B)
  }
  def stopWrite() = {
    port.wr.poke(false.B)
  }
  def expectReadData(data: BigInt) = {
    val obtained = port.rdData.peek().litValue
    assert(obtained == data, s"[ReadData] Expected $data, got $obtained")
  }

  def awaitAck() = {
    while (port.ack.peek().litToBoolean == false) {
      clock.step(1)
    }
  }
}
class spiPortWriter(port: spiMultiChipIO, clock: Clock, select: Int = 0) {
  var previousSpiClk = false
  def isSpiRisingEdge(): Boolean = {
    port.spiClk.peek().litToBoolean && !previousSpiClk
  }
  def updatePrevSpiClk() = {
    previousSpiClk = port.spiClk.peek().litToBoolean
  }
  def expectChipEnable(value: Boolean) {
    val obtained = port.chipSelect(select).peekBoolean()
    assert(obtained == value, s"Expected $value, got $obtained")
  }
  def sendJEDECID() = {
    val instruction = "h9F".U(8.W) // JEDEC ID Instruction
    val instructionBits = instruction.asBools
    for (i <- 0 until 8) {
      while (!isSpiRisingEdge()) {
        updatePrevSpiClk()
        clock.step(1)
      }
      port.dataIn.poke(instructionBits(i))
      updatePrevSpiClk()
      clock.step(1)
    }
  }
  def setWriteEnable() = {
    val instruction = "h06".U(8.W) // Write Enable Instruction
    val instructionBits = instruction.asBools
    for (i <- 0 until 8) {
      while (!isSpiRisingEdge()) {
        updatePrevSpiClk()
        clock.step(1)
      }
      port.dataIn.poke(instructionBits(i))
      updatePrevSpiClk()
      clock.step(1)
    }
  }

}

class SPIRAMSpec
    extends AnyFlatSpec
    with ChiselScalatestTester {
  behavior of "SPIWriteRead"
  it should "readfromRAM" in {
    test(
      new OffChipMemoryControllerWrapper()
    ) { dut =>
      val clock = dut.clock
      val mcSpiPort = dut.mcSpiPort
      val pipeConFlash = dut.pipeConFlash
      val pipeConRam1 = dut.pipeConRam1
      val pipeConRam2 = dut.pipeConRam2
      val pipeConConfig = dut.pipeConConfig  


      val pipeConWriter = new pipeConWriter(pipeConRam1, clock)
      val spiPortWriter = new spiPortWriter(mcSpiPort, clock, 1)

      // Write to the memory
      pipeConWriter.triggerWrite("h00000000".U(23.W), "hDEADBEEF".U(32.W))
      clock.step(1)
      pipeConWriter.stopWrite()
      pipeConWriter.awaitAck()

      // Read from the memory
      pipeConWriter.triggerRead("h00000000".U(23.W))
      clock.step(1)
      pipeConWriter.stopRead()
      pipeConWriter.awaitAck()
      pipeConWriter.expectReadData("h0".U(32.W).litValue) // TODO, no writer for SPI


    }
  }
}