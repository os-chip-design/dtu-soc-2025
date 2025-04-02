package SpiControllerG4

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chiseltest._

// to run this write in the terminal: sbt "testOnly SpoControllerG4.SpiControllerTest"
class SpiControllerTest extends AnyFlatSpec with ChiselScalatestTester {
  "SPI Controller" should "transmit data correctly" in {
    test(new SpiControllerG4.SpiControllerG4).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      // Basic setup
      c.io.enable.poke(false.B)
      c.io.prescale.poke(4.U) // SPI clock = system clock / 8
      c.io.rw.poke(true.B) // Write-only mode
      c.io.sendLength.poke(8.U) // Send 8 bits
      c.io.cpuWriteData.poke(0xAA000000L.U) // 0xAA in upper 8 bits

      // start transmitting
      c.io.enable.poke(true.B)
      c.clock.step(1)
      c.io.enable.poke(false.B)

      // wait for CS active (low)
      while (c.io.spiCs.peek().litToBoolean) {
        c.clock.step(1)
      }

      // check 8 MOSI bits (MSB first)
      for (i <- 0 until 8) {
        // Wait for falling edge
        while (c.io.spiClk.peek().litToBoolean) {
          c.clock.step(1)
        }
        c.io.spiMosi.expect(((0xAA >> (7 - i)) & 1).U) // Check bit
        c.clock.step(4) // Wait for next falling edge
      }

      // verifie
      c.io.done.expect(true.B)
    }
  }

  it should "receive data correctly" in {
    test(new SpiControllerG4.SpiControllerG4).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      // Basic setup
      c.io.enable.poke(false.B)
      c.io.prescale.poke(4.U) // Same clock as TX test
      c.io.rw.poke(false.B) // Receive mode
      c.io.receiveLength.poke(8.U) // Receive 8 bits
      c.io.sendLength.poke(0.U) // Don't send anything

      // start receiving
      c.io.enable.poke(true.B)
      c.clock.step(1)
      c.io.enable.poke(false.B)

      // wait for CS active
      while (c.io.spiCs.peek().litToBoolean) {
        c.clock.step(1)
      }

      // give MISO 0x55 (01010101)
      val testData = 0x55
      for (i <- 0 until 8) {
        // Wait for falling edge
        while (c.io.spiClk.peek().litToBoolean) {
          c.clock.step(1)
        }
        c.io.spiMiso.poke(((testData >> (7 - i)) & 1).B) // MSB first
        c.clock.step(4) // next falling edge
      }

      // wait for completion
      while (!c.io.done.peek().litToBoolean) {
        c.clock.step(1)
      }

      // verifie
      c.io.cpuReadData.expect(testData.U)
    }
  }
}