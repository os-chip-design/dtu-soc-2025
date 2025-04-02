import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipeConTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConExample" should "perform read and write operations correctly" in {
    test(new PipeConExample(8)) { c =>
      c.wrDataCPU.poke(0x00001111.U)
      c.wrMaskCPU.poke(VecInit(Seq(true.B, true.B, false.B, false.B)))



      c.io.UART.rdData.expect("h00001111".U(32.W))

      
    }
  }
}
