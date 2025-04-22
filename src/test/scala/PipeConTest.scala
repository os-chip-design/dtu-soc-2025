import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipeConExampleTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "PipeConExample"

  it should "perform a simple write and read to the UART peripheral" in {
    test(new PipeConExample(8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val uartAddress = 0x01

      // Test default assignments
      c.io.uartWr.expect(false.B)   // Initially, wr should be false

      // --- Write to UART ---
      c.io.cpuAddress.poke(uartAddress.U)
      c.io.cpuWrData.poke("h0000BABE".U)
      c.io.cpuWr.poke(true.B)
      c.io.cpuRd.poke(false.B)

      c.clock.step() // perform the write

      // Check that the write was received
      c.io.uartWr.expect(true.B)
      c.io.uartWrData.expect("h0000BABE".U)

      c.io.cpuWr.poke(false.B) // clear write

      // --- Simulate UART producing read data ---
//      c.io.uartWrData.poke("hDEADBEEF".U)
//
//      // --- Read from UART ---
//      c.io.cpuAddress.poke(uartAddress.U)
//      c.io.cpuRd.poke(true.B)
//      c.io.cpuWr.poke(false.B)
//      c.clock.step()
//
//      // Check that the CPU sees the data
//      c.io.cpuRdData.expect("hDEADBEEF".U)
//      c.io.cpuRd.poke(false.B)
    }
  }
}
