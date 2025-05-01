import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// Boilerplate stuff - work in progress

class GpioPadTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "The GpioPad"

    it should "pass" in {
        test(
            new GpioPadTop
            ).withAnnotations(Seq(
                IcarusBackendAnnotation,
                WriteVcdAnnotation)
            ) {
            dut => {
                dut.clock.step(10)

                // Lets say it is an output pad first
                //dut.io.OE_N.poke(false.B)
                dut.io.OE_N.poke(false.B)
                dut.clock.step(10)
                
                // Give it some stimuli:
                //dut.io.OUT.poke(true.B)
                dut.io.OUT.poke(true.B)
                dut.clock.step(10)

                // Check the pad if it is high (To be implemented)
                //dut.io.PAD.expect(false.B)
                //dut.PAD.expect(false.B)
                //dut.clock.step(10)

                // Switch to input mode
                //dut.io.OE_N.poke(true.B)
                dut.io.OE_N.poke(true.B)
                dut.clock.step(10)

                // Stimulate the pad
                //dut.io.PAD.poke(true.B)
                //dut.PAD.poke(true.B)
                //dut.clock.step(10)

                // and check the input:
                //dut.io.IN.expect(false.B)
                dut.io.IN.expect(false.B)
                dut.clock.step(10)

                // So now it seems the functionality simulation
                // is working, since this fails:
                // dut.io.IN.expect(true.B)
            }
        }
    }
}