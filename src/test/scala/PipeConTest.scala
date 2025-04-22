import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipeConExampleTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "PipeConExample"

  it should "perform a simple write and read to the UART peripheral" in {
    test(new PipeConExample(8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val uartAddress = 0x01

      // --- Initial state ---
      c.io.uartWr.expect(false.B)

      // --- Write to UART ---
      c.io.cpuAddress.poke(uartAddress.U)
      c.io.cpuWrData.poke("hCAFEBABE".U)
      c.io.cpuWr.poke(true.B)
      c.io.cpuRd.poke(false.B)
      c.clock.step()

      // Check that the UART received the write
      c.io.uartWr.expect(true.B)
      c.io.uartWrData.expect("hCAFEBABE".U)

      // Clear write
      c.io.cpuWr.poke(false.B)
      c.clock.step()

      // --- Simulate UART producing read data ---
      // Inject test read data
      c.io.uartRdDataTest.poke("h0000BEEF".U)

      // --- Read from UART ---
      c.io.cpuAddress.poke(uartAddress.U)
      c.io.cpuRd.poke(true.B)
      c.io.cpuWr.poke(false.B)

      // Wait until the design signals that it's reading from UART
      var rdSeen = false
      for (_ <- 0 until 5) {
        if (c.io.uartRd.peek().litToBoolean) {
          rdSeen = true
        }
        c.clock.step()
      }

      assert(rdSeen, "UART was never read by the CPU!")

      // CPU should now see the UART data
      c.io.cpuRdData.expect("h0000BEEF".U)
      c.io.cpuRd.poke(false.B)
    }
  }

  it should "handle read without write to the UART peripheral" in {
    test(new PipeConExample(8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val uartAddress = 0x01

      // Perform a read operation without any prior write
      c.io.cpuAddress.poke(uartAddress.U)
      c.io.cpuRd.poke(true.B)
      c.io.cpuWr.poke(false.B)
      c.clock.step()

      // Check that the UART did not return any unexpected data
      c.io.cpuRdData.expect(0.U)
    }
  }

  it should "perform multiple write and read operations to the UART peripheral" in {
    test(new PipeConExample(8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val uartAddress = 0x01

      // First Write
      c.io.cpuAddress.poke(uartAddress.U)
      c.io.cpuWrData.poke("h0000AAAA".U)
      c.io.cpuWr.poke(true.B)
      c.io.cpuRd.poke(false.B)
      c.clock.step()

      // Check UART received first write
      c.io.uartWr.expect(true.B)
      c.io.uartWrData.expect("h0000AAAA".U)

      // Second Write
      c.io.cpuWrData.poke("h0000BBBB".U)
      c.io.cpuWr.poke(true.B)
      c.clock.step()

      // Check UART received second write
      c.io.uartWr.expect(true.B)
      c.io.uartWrData.expect("h0000BBBB".U)
      c.clock.step()

      // Reset write signal
      c.io.cpuWr.poke(false.B)
      c.clock.step()

      // Third Write
      c.io.cpuWrData.poke("h0000CCCC".U)
      c.io.cpuWr.poke(true.B)
      c.clock.step()

      // Read from UART again
      c.io.cpuRd.poke(true.B)
      c.clock.step()

      // Expect the third written value
      c.io.cpuRdData.expect("h0000CCCC".U)
    }
  }
  it should "perform write and read with different data sizes" in {
    test(new PipeConExample(8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val uartAddress = 0x01

      // Write 8-bit data
      c.io.cpuAddress.poke(uartAddress.U)
      c.io.cpuWrData.poke("hAA".U)  // 8-bit data
      c.io.cpuWr.poke(true.B)
      c.clock.step()

      // Read back 8-bit data
      c.io.cpuWr.poke(false.B)
      c.io.cpuRd.poke(true.B)
      c.clock.step()
      c.io.cpuRdData.expect("hAA".U)
      c.io.cpuRd.poke(false.B)

      // Write 16-bit data
      c.io.cpuAddress.poke(uartAddress.U)
      c.io.cpuWrData.poke("hBBBB".U)  // 16-bit data
      c.io.cpuWr.poke(true.B)
      c.clock.step()

      // Read back 16-bit data
      c.io.cpuWr.poke(false.B)
      c.io.cpuRd.poke(true.B)
      c.clock.step()
      c.io.cpuRdData.expect("hBBBB".U)
      c.io.cpuRd.poke(false.B)

      // Write 32-bit data
      c.io.cpuAddress.poke(uartAddress.U)
      c.io.cpuWrData.poke("hCCCCCCCC".U)  // 32-bit data
      c.io.cpuWr.poke(true.B)
      c.clock.step()

      // Read back 32-bit data
      c.io.cpuWr.poke(false.B)
      c.io.cpuRd.poke(true.B)
      c.clock.step()
      c.io.cpuRdData.expect("hCCCCCCCC".U)
      c.io.cpuRd.poke(false.B)

      // Write 24-bit data
      c.io.cpuAddress.poke(uartAddress.U)
      c.io.cpuWrData.poke("hFFFFFF".U)  // 24-bit data
      c.io.cpuWr.poke(true.B)
      c.clock.step()

      // Read back 24-bit data
      c.io.cpuWr.poke(false.B)
      c.io.cpuRd.poke(true.B)
      c.clock.step()
      c.io.cpuRdData.expect("hFFFFFF".U)
      c.io.cpuRd.poke(false.B)
      
      
    }
  }


}
