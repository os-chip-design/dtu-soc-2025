import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipeConInterconnectTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConInterconnect" should "instantiate correctly with a file" in {
    // Path to your file, relative to the project root or use absolute path
    val testFile = "C:\\Users\\Andreas\\OneDrive\\kandidat\\Intro_Chip\\dtu-soc-2025\\src\\main\\scala\\c\\hello.bin" // ensure this file exists

    test(new PipeConInterconnect(testFile, addrWidth = 32)) { c =>
        c.clock.setTimeout(0)

        for (i <- 0 until 100) {
          c.clock.step(1)
          println(s"clock: $i")
          val address = c.io.uart.address.peek()
          println(s"Address value is: $address") 
          val wrData = c.io.uart.wrData.peek()
          println(s"WrData value is: $wrData")
          if (c.io.uart.wr.peek() == true.B) {
            val address = c.io.uart.address.peek()
            println(s"Address value is: $address")   
            val wrData = c.io.uart.wrData.peek()
            println(s"WrData value is: $wrData") 
          }       
        }
        if (c.io.uart.wr.peek().litToBoolean) {
          println(s"UART wrote: ${c.io.uart.wrData.peek().litValue.toChar}")
        }
        

      // For now, we just verify it instantiates and doesn't crash
      println("PipeConInterconnect instantiated successfully.")
    }
  }
}
