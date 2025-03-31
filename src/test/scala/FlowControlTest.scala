import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class FlowControlTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "CTS Uart Module"

  "MemoryMappedUart" should "only transmit data when CTS is HIGH" in {
    test(new MemoryMappedUart(10000, 3000, 2, 2)) { dut =>
      dut.io.port.init()
      dut.io.pins.CTS.poke(false.B) // Initially disable transmission
      dut.clock.step(1)

      // Attempt to write data
      dut.io.port.write.poke(true.B)
      dut.io.port.addr.poke(0x00.U)
      dut.io.port.wrData.poke(0x41.U) // 'A' character
      dut.clock.step(1)

      // Since CTS is low, transmission should be blocked
      dut.io.pins.tx.expect(1.B) // UART should remain idle

      // Enable transmission by setting CTS high
      dut.io.pins.CTS.poke(true.B)
      dut.clock.step(1)

      // Now data should be transmitted
      dut.io.pins.tx.expect(0.B, "Expected transmission when CTS is high")



      //      val CTSTest = 0.B
      //      dut.io.pins.CTS.poke(CTSTest)
      //
      //      val expected = 1.B
      //      val actual = dut.io.pins.tx.peek
      //
      //      println(s"expected = $expected, actual = $actual")
      //
      //      dut.io.pins.tx.expect(expected)


    }
  }
}