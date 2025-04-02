import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class FlowControlTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Flow Control Uart"

  "MemoryMappedUart with flow control" should "respect CTS signal" in {
    test(new MemoryMappedUart(10000, 3000, 2, 2)) { dut =>
      val driver = MemoryMappedUartDriver(dut)

      // Simplest test: manually handle loopback
      def performLoopbackFor(steps: Int): Unit = {
        for (_ <- 0 until steps) {
          dut.io.pins.rx.poke(dut.io.pins.tx.peek())
          dut.clock.step()
        }
      }

      // 1. Test send/receive with CTS high
      dut.io.pins.cts.poke(true.B)
      val testByte = 0x55 // (01010101)
      driver.send(testByte.U(8.W))

      // Perform manual loopback for enough cycles to complete transmission
      performLoopbackFor(100)

      assert(driver.hasData, "Should have received data")
      val receivedValue = driver.receive().litValue.toInt
      assert(receivedValue == testByte, s"Expected $testByte but got $receivedValue")


      // 2. Test with CTS low - should not transmit
      dut.io.pins.cts.poke(false.B)
      dut.clock.step(5)

      val blockedByte = 0xAA
      val sentSuccessfully = driver.trySend(blockedByte.U(8.W))
      assert(!sentSuccessfully, "Data should not be sent when CTS is false")

      performLoopbackFor(100)
      assert(!driver.hasData, "No data should be received when CTS is false")

      // 3. Test with CTS high again - should transmit now
      dut.io.pins.cts.poke(true.B)
      dut.clock.step(5)

      driver.send(blockedByte.U(8.W))
      performLoopbackFor(100)

      assert(driver.hasData, "Should have received data after CTS is enabled")
      val received2Value = driver.receive().litValue.toInt
      assert(received2Value == blockedByte, s"Expected $blockedByte but got $received2Value")
    }
  }
}