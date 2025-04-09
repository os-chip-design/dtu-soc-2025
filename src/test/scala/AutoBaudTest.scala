import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3._

class AutoBaudTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "AutoBaud"

  "AutoBaud" should "detect baud rate" in {
    test(new AutoBaud()) { dut =>
      val clockFreq = 100000
      val testBaud = 3400
      val clockCyclesPB = (clockFreq / testBaud)
      var clockCount = 0
      var bitCount = 0

      var rxState = true

      while (bitCount <= 14){
        if(clockCount % clockCyclesPB == 0){
          bitCount = bitCount + 1
          rxState = !rxState
          //println(s"changing rx to: ${dut.io.rx.peek().litValue}")
        }
        clockCount = clockCount + 1
        dut.io.rx.poke(rxState)
        dut.clock.step()
        //println(s"At cycle $bitCount, baudRate is: ${dut.io.baudOut.peek().litValue}")
      }

      dut.io.baudOut.expect(clockFreq/clockCyclesPB)

    }
  }
}