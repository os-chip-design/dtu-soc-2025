import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.io.{File, IOException}


class PipeConInterconnectTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConInterconnect" should "instantiate correctly with a file" in {
    // Path to your file, relative to the project root or use absolute path
    val testFile = "C:\\Users\\Andreas\\OneDrive\\kandidat\\Intro_Chip\\dtu-soc-2025\\src\\main\\scala\\c\\hello.bin" // ensure this file exists

    test(new PipeConInterconnect(testFile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>       // Enable VCD tracing

      c.clock.setTimeout(0)

      for (i <- 0 until 1000) {
        c.clock.step(1)
        println(s"clock: $i")
        
        val address = c.io.device(0).address.peek()
        println(s"Address value is: $address") 
        val wrData = c.io.device(0).wrData.peek()
        println(s"WrData value is: $wrData")
        
        if (c.io.device(0).wr.peek() == true.B) {
          val address = c.io.device(0).address.peek()
          println(s"Address value is: $address")   
          val wrData = c.io.device(0).wrData.peek()
          println(s"WrData value is: $wrData") 
        }       
      }

      if (c.io.device(0).wr.peek().litToBoolean) {
        println(s"UART wrote: ${c.io.device(0).wrData.peek().litValue.toChar}")
      }

      // For now, we just verify it instantiates and doesn't crash
      println("PipeConInterconnect instantiated successfully.")
    }
  }
  "PipeConExample" should "instantiate correctly with a file" in {
    // Path to your file, relative to the project root or use absolute path
    val testFile = "C:\\Users\\Andreas\\OneDrive\\kandidat\\Intro_Chip\\dtu-soc-2025\\src\\main\\scala\\c\\hello.bin" // ensure this file exists

    test(new PipeConExample(testFile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation)) { c =>       // Enable VCD tracing

      c.clock.setTimeout(0)

      for (i <- 0 until 1000) {
        c.clock.step(1)
      }
      // For now, we just verify it instantiates and doesn't crash
      println("PipeConInterconnect instantiated successfully.")
    }
  }
}
