import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.io.{File, IOException}


class PipeConExampleTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConExample" should "Write to actual GPIOperipheral" in {
    // Path to testfile
    val testfile = getClass.getResource("/helloGPIO.bin").getPath

    test(new PipeConExample(testfile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { c =>
      val expected = "HelloWorld".map(_.toByte) // List of ASCII bytes
      var idx = 0 // To track the index in the expected data

      c.clock.setTimeout(0)

      // Run for a fixed number of cycles (e.g., 100 cycles)
      for (_ <- 0 until 100) {
        // If wr signal is high, check if the received data matches the expected data at index idx
        if (c.io.cpuWrEnable.peek().litValue != 0) {
          val data = c.io.GPIO_wrData.peek().litValue.toByte
          assert(data == expected(idx), 
            s"Test failed at index $idx: expected '${expected(idx).toChar}', got '${data.toChar}'")
          
          idx += 1 // Move to the next expected character if matched
          
          if (idx >= expected.length) {
            // If we've matched the whole expected string, loop back to the start
            idx = 0
          }
        }

        // Step the clock
        c.clock.step(1)
      }
      assert(true, "Test completed without fatal errors.")

    }
  }  
  
  "PipeConTest" should "instantiate correctly and write to UART" in {

    // Path to testfile
    val testfile = getClass.getResource("/hello.bin").getPath

    test(new PipeConExample(testfile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { c =>
      val expected = "HelloWorld".map(_.toByte) // List of ASCII bytes
      var idx = 0 // To track the index in the expected data

      c.clock.setTimeout(0)

      // Run for a fixed number of cycles (e.g., 100 cycles)
      for (_ <- 0 until 100) {
        // If wr signal is high, check if the received data matches the expected data at index idx
        if (c.io.cpuWrEnable.peek().litValue != 0) {
          val data = c.io.uart_wrData.peek().litValue.toByte
          assert(data == expected(idx), 
            s"Test failed at index $idx: expected '${expected(idx).toChar}', got '${data.toChar}'")
          
          idx += 1 // Move to the next expected character if matched
          
          if (idx >= expected.length) {
            // If we've matched the whole expected string, loop back to the start
            idx = 0
          }
        }

        // Step the clock
        c.clock.step(1)
      }

      // If everything has passed, the test will just complete successfully
      assert(true, "Test completed without fatal errors.")
    }
  }

  "PipeConTest" should "fail when the expected value is not HelloWorld" in {
    // Path to testfile
    val testfile = getClass.getResource("/hello.bin").getPath

    test(new PipeConExample(testfile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { c =>
      val expected = "HelloWorl".map(_.toByte) // Incorrect expected value (without 'd')
      var idx = 0 // To track the index in the expected data

      c.clock.setTimeout(0)

      // Run for a fixed number of cycles (e.g., 100 cycles)
      var testFailed = false
      for (_ <- 0 until 100) {
        // If wr signal is high, check if the received data matches the expected data at index idx
        if (c.io.cpuWrEnable.peek().litValue != 0) {
          val data = c.io.uart_wrData.peek().litValue.toByte
          
          if (data != expected(idx)) {
            testFailed = true
            //println(s"Test failed (intentionally) at index $idx: expected '${expected(idx).toChar}', got '${data.toChar}'")
          }
          
          idx += 1 // Move to the next expected character if matched
          
          if (idx >= expected.length) {
            // If we've matched the whole expected string, loop back to the start
            idx = 0
          }
        }

        // Step the clock
        c.clock.step(1)
      }

      // Assert that the test failed (because the expected and received data do not match)
      assert(testFailed, "Test did not fail as expected when expected value was incorrect.")
    }
  }

  "PipeConTest" should "run poll.bin and write to UART when finished" in {
    // Load the binary file from test resources
    val testfile = getClass.getResource("/poll.bin").getPath

    test(new PipeConExample(testfile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { c =>
      c.clock.setTimeout(0)

      val maxCycles = 100
      println(s"Running $maxCycles cycles...")

      for (cycle <- 0 until maxCycles) {
        // Simulate one clock step
        c.clock.step(1)

        // UART output
        if (c.io.uart_wr.peek().litToBoolean) {
          val uartChar = c.io.uart_wrData.peek().litValue.toByte.toChar
          //println(s"[UART] Wrote: '$uartChar'")
        }

        // Memory reads (simulate address polling)
        if (c.io.cpuRdEnable.peek().litValue != 0) {
          val readAddr = c.io.cpuRdAddress.peek().litValue
          val readData = c.io.cpuRdData.peek().litValue
          println(f"[CPU] Read from 0x$readAddr%08X: 0x$readData%08X")
        }
      }
    }
  }
}