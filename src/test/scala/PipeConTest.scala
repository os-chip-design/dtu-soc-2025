import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipeConExampleTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "PipeConExample"

  it should "simulate a UART write and read" in {
  test(new PipeConExample(8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val uartAddress = 0x01

      // Perform a read operation from the CPU
      c.io.uartRdDataTest.poke("h0000BEEF".U)  // Simulate data being available to read
      c.io.uartRd.poke(true.B)  // Activate UART read signal
      c.clock.step()

      // Monitor the UART read signal from the interconnect
      var readDetected = false
      while (!readDetected) {
        c.clock.step(1)
        if (c.io.uartRd.peek().litToBoolean) {  // Check if UART read signal is active
          println("UART read detected from the CPU")
          readDetected = true
        } else {
          println("Nothing detected...")
        }
      }

      //var readDetected2 = false
      //while (!readDetected2) {
      //  c.clock.step(1)
      //  if (c.interconnect.cpu.io.dmem.wrEnable(0).peek().litToBoolean) {  // Check if UART read signal is active
      //    println("wrEnable detected")
      //    readDetected2 = true
      //  } else
      //  if (c.interconnect.io.rdEnableTest.litToBoolean) {
      //    println("rdEnable detected")
      //    readDetected2 = true
      //  } else {
      //    println("Nothing detected...")
      //  }
      //}
    }
  }


  //it should "handle read without write to the UART peripheral" in {
  //  test(new PipeConExample(8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
  //    val uartAddress = 0x01
//
  //    // Perform a read operation without any prior write
  //    c.io.cpuAddress.poke(uartAddress.U)
  //    c.io.cpuRd.poke(true.B)
  //    c.clock.step()
//
  //    // Check that the UART did not return any unexpected data
  //    c.io.cpuRdData.expect(0.U)
  //  }
  //}
//
  //it should "perform multiple write and read operations to the UART peripheral" in {
  //  test(new PipeConExample(8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
  //    val uartAddress = 0x01
//
  //    // First Write
  //    c.io.cpuAddress.poke(uartAddress.U)
  //    c.io.cpuWrMask.poke("b0011".U)
  //    c.io.cpuWrData.poke("h0000AAAA".U)
  //    c.io.cpuRd.poke(false.B)
  //    c.clock.step()
//
  //    c.io.uartWrMask.expect("b0011".U)
  //    c.io.uartWrData.expect("h0000AAAA".U)
//
  //    // Second Write
  //    c.io.cpuWrMask.poke("b0011".U)
  //    c.io.cpuWrData.poke("h0000BBBB".U)
  //    c.clock.step()
//
  //    c.io.uartWrMask.expect("b0011".U)
  //    c.io.uartWrData.expect("h0000BBBB".U)
  //    c.clock.step()
//
  //    // Reset write signal
  //    c.io.cpuWrMask.poke(0.U)
  //    c.clock.step()
//
  //    // Third Write
  //    c.io.cpuWrMask.poke("b0011".U)
  //    c.io.cpuWrData.poke("h0000CCCC".U)
  //    c.clock.step()
//
  //    // Read from UART again
  //    c.io.cpuRd.poke(true.B)
  //    c.clock.step()
//
  //    // Expect the third written value
  //    c.io.cpuRdData.expect("h0000CCCC".U)
  //  }
  //}
//
  //it should "perform write and read with different data sizes" in {
  //  test(new PipeConExample(8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
  //    val uartAddress = 0x01
//
  //    // Write 8-bit data
  //    c.io.cpuAddress.poke(uartAddress.U)
  //    c.io.cpuWrMask.poke("b0001".U)
  //    c.io.cpuWrData.poke("hAA".U)
  //    c.clock.step()
//
  //    // Read back 8-bit data
  //    c.io.cpuWrMask.poke(0.U)
  //    c.io.cpuRd.poke(true.B)
  //    c.clock.step()
  //    c.io.cpuRdData.expect("hAA".U)
  //    c.io.cpuRd.poke(false.B)
//
  //    // Write 16-bit data
  //    c.io.cpuAddress.poke(uartAddress.U)
  //    c.io.cpuWrMask.poke("b0011".U)
  //    c.io.cpuWrData.poke("hBBBBBBBB".U)
  //    c.clock.step()
//
  //    // Read back 16-bit data
  //    c.io.cpuWrMask.poke(0.U)
  //    c.io.cpuRd.poke(true.B)
  //    c.clock.step()
  //    c.io.cpuRdData.expect("h0000BBBB".U)
  //    c.io.cpuRd.poke(false.B)
//
  //    // Write 32-bit data
  //    c.io.cpuAddress.poke(uartAddress.U)
  //    c.io.cpuWrMask.poke("b1111".U)
  //    c.io.cpuWrData.poke("hCCCCCCCC".U)
  //    c.clock.step()
//
  //    // Read back 32-bit data
  //    c.io.cpuWrMask.poke(0.U)
  //    c.io.cpuRd.poke(true.B)
  //    c.clock.step()
  //    c.io.cpuRdData.expect("hCCCCCCCC".U)
  //    c.io.cpuRd.poke(false.B)
//
  //    // Write 24-bit data
  //    c.io.cpuAddress.poke(uartAddress.U)
  //    c.io.cpuWrMask.poke("b0111".U)
  //    c.io.cpuWrData.poke("hFFFFFF".U)
  //    c.clock.step()
//
  //    // Read back 24-bit data
  //    c.io.cpuWrMask.poke(0.U)
  //    c.io.cpuRd.poke(true.B)
  //    c.clock.step()
  //    c.io.cpuRdData.expect("hFFFFFF".U)
  //    c.io.cpuRd.poke(false.B)
  //  }
  //}
//
  //it should "handle write with wrMask and read the correct data" in {
  //  test(new PipeConExample(8)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
  //    val SPIAddress = 0x02
//
  //    c.io.cpuAddress.poke(SPIAddress.U)
  //    c.io.cpuWrMask.poke("b0111".U)
  //    c.io.cpuWrData.poke("hFFFFFFFF".U)
  //    c.clock.step()
//
  //    c.io.cpuWrMask.poke(0.U)
  //    c.io.cpuRd.poke(true.B)
  //    c.clock.step()
  //    c.io.cpuRdData.expect("h00FFFFFF".U)
  //    c.io.cpuRd.poke(false.B)
  //  }
  //}
}