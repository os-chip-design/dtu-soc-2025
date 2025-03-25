import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipeConTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConExample" should "perform read and write operations correctly" in {
    test(new PipeConExample(8)) { c =>
      // Write data to address 0
      c.io.pipe.wr.poke(true.B)
      c.io.pipe.address.poke(0.U)
      c.io.pipe.wrMask.poke("b0001".U)
      c.io.pipe.wrData.poke(0x12345678.U)
      c.io.pipe.ack.expect(true.B)  // Expect ack to be high after write

      c.clock.step(1)  // Allow time for write

      c.io.pipe.wr.poke(0.U)
      c.io.pipe.rd.poke(true.B)
      c.io.pipe.address.poke(0.U)
      c.io.pipe.rdData.expect(0x00000078.U) 
      c.io.pipe.ack.expect(true.B)  // Expect ack to be high after write
      
      
    }
  }
}
