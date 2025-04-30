import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
class configWriter(
    port: configIO,
    clock: Clock
) {
  def setJEDEC(value: Boolean) = {
    port.jedec.poke(value)
  }
  def setClear(value: Boolean) = {
    port.clear.poke(value)
  }
  def setTargetFlash(value: Boolean) = {
    port.targetFlash.poke(value)
  }
  def setClockDivision(value: UInt) = {
    port.clockDivision.poke(value)
  }
  def setMode(value: Boolean) = {
    port.mode.poke(value)
  }
}
class configReader(
    port: configIO,
    clock: Clock
) {
  def getJEDEC(): Boolean = {
    port.jedec.peek().litToBoolean
  }
  def getClear(): Boolean = {
    port.clear.peek().litToBoolean
  }
  def getTargetFlash(): Boolean = {
    port.targetFlash.peek().litToBoolean
  }
  def getClockDivision(): UInt = {
    port.clockDivision.peek()
  }
  def getMode(): Boolean = {
    port.mode.peek().litToBoolean
  }
}

class configSpec
    extends AnyFlatSpec
    with ChiselScalatestTester {
  behavior of "SPIWriteRead"
  it should "readOnTheConfig File" in {
    test(
      new OffChipMemoryConfig(clockWidth = 8, addrWidth = 24)
    ) { dut =>
      val clock = dut.clock
      val pipeCon = dut.pipeCon
      val config = dut.config
      val configWriter = new configWriter(config, clock)
      val pipeConWriter = new pipeConWriter(pipeCon, clock)
      assert(pipeCon.ack.peek().litToBoolean == false)
      pipeCon.rd.poke(true.B)
      pipeCon.wr.poke(false.B)
      pipeCon.wrData.poke(0.U)
      
      clock.step(1)
      assert(pipeCon.ack.peek().litToBoolean == true)
      assert(pipeCon.rdData.peek().litValue == 36)
      assert(config.jedec.peek().litToBoolean == false)
      assert(config.clear.peek().litToBoolean == false)
      assert(config.targetFlash.peek().litToBoolean == true)



      


    }
  }
}