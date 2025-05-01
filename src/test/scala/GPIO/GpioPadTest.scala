import chisel3._
import chisel3.util._
//import chisel3.experimental._
import chisel3.experimental.{Analog, attach}
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
                dut.clock.step(10)

                // Switch to input mode
                //dut.io.OE_N.poke(true.B)
                dut.io.OUT.poke(false.B)
                dut.io.OE_N.poke(true.B)
                dut.clock.step(10)

                //dut.io.IN.expect(false.B)

                // Stimulate the pad
                //val test_port = 1.U
                //attach(dut.gpioPad.io.PAD, test_port)
                //dut.gpioPad.io.PAD.poke(true.B)
                //dut.io.test_pad(true.B)
                //dut.PAD.poke(true.B)

                // val padDriver = IO(Input(UInt(1.W)))
                // val pad = IO(Analog(1.W))

                // attach(dut.gpioPad.io.PAD, pad)

                // // Convert UInt -> Analog for attaching
                // val driverWire = Wire(Analog(1.W))
                // driverWire := DontCare // must be initialized

                // // Attach UInt through an intermediate Analog
                // val driver = Wire(UInt(1.W))
                // driver := padDriver

                // // Use Verilog trick: assign through attach
                // attach(driver, pad)

                // padDriver.poke(true.B)
                //attach(WireInit(1.U(1.W)).asTypeOf(Analog(1.W)), dut.gpioPad.io.PAD)
                //attach(WireInit(1.U(1.W)).asTypeOf(Analog(1.W)), dut.io.test_pad)
                dut.clock.step(10)

                // and check the input:
                //dut.io.IN.expect(false.B)
                dut.io.IN.expect(true.B)
                dut.clock.step(10)

                // So now it seems the functionality simulation
                // is working, since this fails:
                // dut.io.IN.expect(true.B)
            }
        }
    }
}