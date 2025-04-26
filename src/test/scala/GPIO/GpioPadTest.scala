import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// Boilerplate stuff - work in progress

class GpioPadTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "The GpioPad"

    it should "pass" in {
        test(new GpioPadTop) {
            dut => {
                // Lets say it is an output pad first
                dut.io.OE_N.poke(false.B)
                // Give it some stimuli:
                dut.io.OUT.poke(true.B)

                // Check the pad if it is high (To be implemented)
                //dut.io.PAD.expect(false.B)

                // Switch to input mode
                dut.io.OE_N.poke(true.B)

                // Stimulate the pad
                //dut.io.PAD.poke(true.B)

                // and check the input:
                dut.io.IN.expect(false.B)

                // So now it seems the functionality simulation
                // is working, since this fails:
                // dut.io.IN.expect(true.B)
            }
        }
    }
}