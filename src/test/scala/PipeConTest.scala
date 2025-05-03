import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.io.{File, IOException}


class PipeConExampleTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConExample" should "svart" in {
    // Path to testfile
    val testfile = getClass.getResource("/hello.bin").getPath

    test(new PipeConExample(testfile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { c =>
      val expected = "HelloWorld".map(_.toByte) // List of ASCII bytes
      var idx = 0 // To track the index in the expected data

      c.clock.setTimeout(0)

      // Run for a fixed number of cycles (e.g., 100 cycles)
      for (_ <- 0 until 100) {
        // If wr signal is high, check if the received data matches the expected data at index idx
        c.clock.step(1)
      }
    }
  }  
  "PipeConTest" should "pass when the expected value is HelloWorld" in {
    runMismatchTest("HelloWorld", shouldFail = false)
  }
  "PipeConTest" should "fail when the expected value is not HelloWorld" in {
    runMismatchTest("HelloWorl", shouldFail = true)
  }
  "PipeConTest" should "fail when the expected value is HelloWorldd" in {
    runMismatchTest("HelloWorldd", shouldFail = true)
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
          c.clock.step(1) // step before evaluating read data from cpu
          val readData = c.io.cpuRdData.peek().litValue
          println(f"[CPU] Read from 0x$readAddr%08X: 0x$readData%08X")
        }
      }
    }
  }

  // Helper function for running multiple "expected" strings
  def runMismatchTest(expectedString: String, shouldFail: Boolean): Unit = {
    val testfile = getClass.getResource("/hello.bin").getPath

    test(new PipeConExample(testfile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { c =>
      val expected = expectedString.map(_.toByte)
      var idx = 0
      var testFailed = false

      c.clock.setTimeout(0)

      for (_ <- 0 until 100) {
        if (c.io.cpuWrEnable.peek().litValue != 0) {
          val data = c.io.uart_wrData.peek().litValue.toByte

          if (data != expected(idx)) {
            testFailed = true
          }

          idx += 1
          if (idx >= expected.length) {
            idx = 0
          }
        }

        c.clock.step(1)
      }

      if (shouldFail)
        assert(testFailed, "Test did not fail as expected when expected value was incorrect.")
      else
        assert(!testFailed, "Test failed unexpectedly when expected value was correct.")
    }
  }
}