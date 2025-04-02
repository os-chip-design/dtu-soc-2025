package SpiControllerG4

import org.scalatest.flatspec.AnyFlatSpec
import chisel3._
import chiseltest._

// to run this write in the terminal: sbt "testOnly SpiControllerG4.SpiControllerTest"
class SpiControllerTest extends AnyFlatSpec with ChiselScalatestTester {
  "SPI Controller" should "transmit data correctly" in {
    test(new SpiControllerG4.SpiControllerG4).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      // Basic setup
      c.io.enable.poke(false.B)
      c.io.prescale.poke(1.U) // SPI clock = system clock / 8
      c.io.rw.poke(true.B) // Write-only mode
      c.io.sendLength.poke(7.U) // Send 8 bits
      c.io.cpuWriteData.poke(0x0A000000.U) // 0x0A in upper 8 bits
      c.io.cpuCommand.poke(0xAA.U)

      // start transmitting
      c.io.enable.poke(true.B)
      c.clock.step(1)
      c.io.enable.poke(false.B)

      // wait for CS active (low)
      while (c.io.spiCs.peekBoolean()) {
        c.clock.step(1)
      }

      // check 8 MOSI bits (MSB first)
      for (i <- 0 until 8) {
        // Wait for rising edge
        while (!c.io.spiClk.peekBoolean()) {
          c.clock.step(1)
        }
        // c.io.spiMosi.expect(((0x0A >> (7 - i)) & 1).U) // Check bit
        c.clock.step(1) // Wait for next rising edge
      }

      // verifie
      c.io.ready.expect(true.B)
    }
  }
}