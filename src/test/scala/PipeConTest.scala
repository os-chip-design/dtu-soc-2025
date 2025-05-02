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
        c.clock.setTimeout(0)
        for (i <- 0 until 100) {
            if (c.io.cpu.ack.peek().litToBoolean) {
            //    // Directly compare the value and convert to hex for the error message
            //    val wrDataValue = c.io.uart.wrData.peek()
            //    assert(wrDataValue === 0x48.U, s"Test failed: Expected 0x48 but got 0x${wrDataValue.toString(16)}")
            }
            c.clock.step(1)     
        }
        if (c.io.device(0).wr.peek() == true.B) {
          println(s"UART wrote: ${c.io.device(0).wrData.peek().litValue.toChar}")
        }
      // For now, we just verify it instantiates and doesn't crash
      println("PipeConInterconnect instantiated successfully.")
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
      println("something was successfull.")
    }
  }
}
