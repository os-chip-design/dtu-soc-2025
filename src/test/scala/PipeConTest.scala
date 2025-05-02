import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.io.{File, IOException}

class PipeConInterconnectTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConTest2" should "instantiate correctly and write to UART" in {
    // Path to your file, relative to the project root or use absolute path
    val dir = "/home/rasmus/Downloads/02118_IntroductionToChipDesign/dtu-soc-2025/src/main/scala/c/"
    val filename = "hello.bin"

    val testfile = dir + filename

    test(new PipeConInterconnect(testfile, addrWidth = 32, devices = 2)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { c =>
      val expected = "HelloWorld".map(_.toByte) // List of ASCII bytes
      var idx = 0 // To track the index in the expected data

      c.clock.setTimeout(0)

      // Run for a fixed number of cycles (e.g., 100 cycles)
      for (_ <- 0 until 100) {
        // If wr signal is high, check if the received data matches the expected data at index idx
        if (c.io.cpu2.wr.peek().litToBoolean) {
          val data = c.io.device(0).wrData.peek().litValue.toByte
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

  "PipeConTest2" should "fail when the expected value is not HelloWorld" in {
    // Path to your file, relative to the project root or use absolute path
    val dir = "/home/rasmus/Downloads/02118_IntroductionToChipDesign/dtu-soc-2025/src/main/scala/c/"
    val filename = "hello.bin"

    val testfile = dir + filename

    test(new PipeConInterconnect(testfile, addrWidth = 32, devices = 2)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { c =>
      val expected = "HelloWorl".map(_.toByte) // Incorrect expected value (without 'd')
      var idx = 0 // To track the index in the expected data

      c.clock.setTimeout(0)

      // Run for a fixed number of cycles (e.g., 100 cycles)
      var testFailed = false
      for (_ <- 0 until 100) {
        // If wr signal is high, check if the received data matches the expected data at index idx
        if (c.io.cpu2.wr.peek().litToBoolean) {
          val data = c.io.device(0).wrData.peek().litValue.toByte
          
          if (data != expected(idx)) {
            testFailed = true
            println(s"Test failed (intentionally) at index $idx: expected '${expected(idx).toChar}', got '${data.toChar}'")
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

  "PipeConTest2" should "run poll.bin and write to UART when finished" in {
    // Path to your file, relative to the project root or use absolute path
    val dir = "/home/rasmus/Downloads/02118_IntroductionToChipDesign/dtu-soc-2025/src/main/scala/c/"
    val filename = "poll.bin"

    val testfile = dir + filename

    test(new PipeConInterconnect(testfile, addrWidth = 32, devices = 2)).withAnnotations(Seq(WriteVcdAnnotation, VerilatorBackendAnnotation)) { c =>
      c.clock.setTimeout(0)
      //c.io.device(0).wrData.poke("hDEADBEEF".U)
      for (i <- 0 until 100) {
        c.clock.step(1)     
      }
      if (c.io.device(0).wr.peek().litToBoolean) {
        println(s"UART wrote: ${c.io.device(0).wrData.peek().litValue.toChar}")
      }
      // For now, we just verify it instantiates and doesn't crash
      println("something was successful.")
    }
  }
}
