import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipeConExampleTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConExample" should "perform read and write correctly" in {
    test(new PipeConExample(8)) { c =>
      // Initial state check
      // Ensure that the initial read data from the CPU is zero
      c.io.cpuRdData.expect(0.U)
      c.io.cpuWr.expect(false.B)
      c.io.cpuRd.expect(false.B)
      c.io.cpuAddress.expect(0.U)

      // Write a value to UART via the interconnect
      val writeData = 0x00000001.U
      c.io.cpuAddress.poke(0x01.U)  // UART address
      c.io.cpuWrData.poke(writeData)
      c.io.cpuWr.poke(true.B)
      c.io.cpuRd.poke(false.B)

      // Wait for one cycle to simulate the write
      c.clock.step(1)

      // Check if the UART receives the write data (check UART register)
      c.io.uartWr.expect(true.B)  // UART should have received a write signal
      c.io.uartWrData.expect(writeData)  // UART should store the data

      // Now perform a read operation
      c.io.cpuAddress.poke(0x01.U)  // UART address
      c.io.cpuWr.poke(false.B)  // Write disabled for read
      c.io.cpuRd.poke(true.B)  // Read enabled

      // Wait for one cycle to simulate the read
      c.clock.step(1)

      // Check if the data read from UART is correct
      c.io.cpuRdData.expect(writeData)  // CPU should receive the same value that was written to UART

      // Ensure that no write operation is happening during read
      c.io.uartRd.expect(true.B)  // UART should have been read
      c.io.uartWr.expect(false.B)  // No write should be happening
    }
  }
}
