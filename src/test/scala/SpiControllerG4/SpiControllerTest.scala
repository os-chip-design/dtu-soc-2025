package SpiControllerG4

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import chiseltest.simulator.WriteVcdAnnotation

// to run this write in the terminal: sbt "testOnly SpiControllerG4.SpiControllerTest"

class SpiControllerTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  // Helper function for SPI mode tests
  private def runSpiTest(mode: Int, prescale: Int = 1) = {
    test(new SpiControllerG4.SpiController)
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        // Test parameters
        val sendLen = 8
        val recvLen = 8
        val waitCycles = 0
        val txValue = 0xAA
        val expectedMosi = 0xAA
        val expectedMiso = 0x55

        // Determine edge types based on SPI mode
        val (initialClkState, mosiCaptureEdge, misoSampleEdge) = mode match {
          case 0 => (false.B, false.B, true.B) // MOSI: falling, MISO: rising
          case 1 => (false.B, true.B, false.B) // MOSI: rising,  MISO: falling
          case 2 => (true.B, true.B, false.B) // MOSI: rising,  MISO: falling
          case 3 => (true.B, false.B, true.B) // MOSI: falling, MISO: rising
        }

        // Initialize inputs
        dut.io.enable.poke(false.B)
        dut.io.txData.poke(txValue.U)
        dut.io.sendLength.poke(sendLen.U)
        dut.io.receiveLength.poke(recvLen.U)
        dut.io.numWaitCycles.poke(waitCycles.U)
        dut.io.prescale.poke(prescale.U)
        dut.io.mode.poke(mode.U)
        dut.io.spiMiso.poke(false.B)


        // Start transaction
        dut.io.enable.poke(true.B)
        dut.clock.step(1)
        dut.io.enable.poke(false.B)

        // Wait for transaction to start
        while (dut.io.ready.peek().litToBoolean) {
          dut.clock.step(1)
        }

        // Tracking variables
        var capturedMosi = 0
        var capturedMiso = 0
        var bitCount = 0
        var prevClk = initialClkState.litToBoolean

        while (!dut.io.spiCs.peek().litToBoolean) {
          dut.clock.step(1)
        }

        // Run until transaction completes
        while (!dut.io.done.peek().litToBoolean) {
          val currentClk = dut.io.spiClk.peek().litToBoolean

          // Detect MOSI capture edge
          if (prevClk != currentClk) {
            if (mosiCaptureEdge.litToBoolean == currentClk) {
              // Capture MOSI bit
              val mosiBit = dut.io.spiMosi.peek().litToBoolean
              capturedMosi = (capturedMosi << 1) | (if (mosiBit) 1 else 0)

            }

            if (misoSampleEdge.litToBoolean == currentClk) {
              // Provide stable MISO bit before sampling
              val misoBit = (expectedMiso >> ((recvLen - 1) - (bitCount % 8))) & 1
              dut.io.spiMiso.poke((misoBit != 0).B)
              bitCount += 1

              // Capture MISO bit
              val misoCaptured = dut.io.spiMiso.peek().litToBoolean
              capturedMiso = (capturedMiso << 1) | (if (misoCaptured) 1 else 0)
            }
          }

          prevClk = currentClk
          dut.clock.step(1)
        }

        // Mask results to MSB bits (our test case size)
        val finalMosi = (capturedMosi >> (recvLen + waitCycles)) & ((1 << sendLen) - 1)
        val finalMiso =
          if (mode == 1 || mode == 3) {
            (capturedMiso >> (sendLen + waitCycles - 1)) & ((1 << recvLen) - 1)
          } else{
            (capturedMiso >> (sendLen + waitCycles)) & ((1 << recvLen) - 1)
          }


        println(f"Mode $mode:")
        println(f"  MOSI: 0x$finalMosi%02X (Expected: 0x$expectedMosi%02X)")
        println(f"  MISO: 0x$finalMiso%02X (Expected: 0x$expectedMiso%02X)")

        // Verify results
        finalMosi mustBe expectedMosi
        finalMiso mustBe expectedMiso
      }
  }

  "SPI Controller" should "handle Mode 0 (CPOL=0, CPHA=0)" in {
    runSpiTest(0)
  }

  it should "handle Mode 1 (CPOL=0, CPHA=1)" in {
    runSpiTest(1)
  }

  it should "handle Mode 2 (CPOL=1, CPHA=0)" in {
    runSpiTest(2)
  }

  it should "handle Mode 3 (CPOL=1, CPHA=1)" in {
    runSpiTest(3)
  }
}