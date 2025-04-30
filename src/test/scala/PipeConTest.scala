import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipeConInterconnectTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConInterconnect" should "instantiate correctly with a file" in {
    // Path to your file, relative to the project root or use absolute path
    val testFile = "C:\\Users\\Andreas\\OneDrive\\kandidat\\Intro_Chip\\dtu-soc-2025\\src\\main\\scala\\c\\hellos.bin" // ensure this file exists

    test(new PipeConInterconnect(testFile, addrWidth = 32)) { c =>
      // You can poke and expect c.io signals here if they exist
      // Example:
      // c.io.someInput.poke(42.U)
      // c.io.someOutput.expect(42.U)
        c.clock.setTimeout(0)

        for (i <- 0 until 100) {
          c.clock.step(1)
          val address = c.io.uart.address.peek()
          println(s"Address value is: $address") 
          if (c.io.uart.wr.peek() == true.B) {
            val address = c.io.uart.address.peek()
            println(s"Address value is: $address")   
            val wrData = c.io.uart.wrData.peek()
            println(s"WrData value is: $wrData") 
          }       
        }
        

      // For now, we just verify it instantiates and doesn't crash
      println("PipeConInterconnect instantiated successfully.")
    }
  }
}
