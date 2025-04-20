import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipeConTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConExample" should "perform read and write operations correctly" in {
    test(new PipeConExample(8)) { c => 
      // Step the clock
      def step() = c.clock.step()

      // Write some data to the UART address
      //c.io.UART.wrData.poke(0x12345678.U)   // Write data to wrData
      //c.io.UART.wrMask(0).poke(true.B)      // Set write mask to write the first byte
      //c.io.UART.wr.poke(true.B)             // Assert write signal

    }
  }
}