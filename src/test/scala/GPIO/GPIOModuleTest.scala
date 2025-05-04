import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class GPIOModuleTest extends AnyFlatSpec with ChiselScalatestTester {

    behavior of "GPIOModule"

    // function to check test outputs
    def checkTestOutputs(dut: GPIOModule, expectedOutput: Bool, expectedOE_N: Bool): Unit = {
        dut.io.test_OUT.get.expect(expectedOutput)
        dut.io.test_OE_N.get.expect(expectedOE_N)
    }

    // function to set default inputs of module
    def setDefaultInputs(dut: GPIOModule): Unit = {
        dut.io.drivestrength.poke(0.U)
        dut.io.pullup_en.poke(false.B)
        dut.io.pulldown_en.poke(false.B)
        dut.io.opendrain_en.poke(false.B)
        dut.io.pwm_div.poke(1.U)
        dut.io.duty_cycle.poke(0.U)
        dut.io.pwm_period.poke(1.U)
        dut.io.pwm_polarity.poke(false.B)
        dut.io.test_PAD_IN.get.poke(false.B)
    }

    it should "act as a simple GPIO output" in {
        test(new GPIOModule(testMode = true)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            setDefaultInputs(dut)
            
            // config
            dut.io.gpio_direction.poke(true.B)
            dut.io.pwm_en.poke(false.B)         // disable pwm
            dut.io.gpio_output.poke(false.B)  
            dut.clock.step(1)
            checkTestOutputs(dut, false.B, false.B)

            // set gpio high
            dut.io.gpio_output.poke(true.B)
            dut.clock.step(1)
            checkTestOutputs(dut, true.B, false.B)

            // set gpio low
            dut.io.gpio_output.poke(false.B)
            dut.clock.step(1)
            checkTestOutputs(dut, false.B, false.B)
        }
    }

    it should "act as a simple GPIO input" in {
        test(new GPIOModule(testMode = true)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            setDefaultInputs(dut)
            
            // config
            dut.io.gpio_direction.poke(false.B) // set as input mode
            dut.io.pwm_en.poke(false.B)         // disable pwm
            dut.io.gpio_output.poke(true.B)     // output value shouldnt matter
            dut.clock.step(1)
            checkTestOutputs(dut, true.B, true.B)

            // simulate some input value from test_PAD_IN and then check it
            dut.io.test_PAD_IN.get.poke(true.B)
            dut.clock.step(1)
            dut.io.gpio_input.expect(true.B)

            dut.io.test_PAD_IN.get.poke(false.B)
            dut.clock.step(1)
            dut.io.gpio_input.expect(false.B)
        }
    }

    it should "output PWM signal when enabled" in {
        test(new GPIOModule(testMode = true)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            setDefaultInputs(dut)
            
            // config
            dut.io.gpio_direction.poke(true.B)
            dut.io.pwm_en.poke(true.B)
            dut.io.gpio_output.poke(false.B)
            dut.io.pwm_div.poke(2.U)
            dut.io.duty_cycle.poke(1.U)
            dut.io.pwm_period.poke(2.U)
            dut.clock.step(1)
            
            // initial state should be low
            checkTestOutputs(dut, false.B, false.B)
            
            // should go high after 2 cycles
            dut.clock.step(2)
            checkTestOutputs(dut, true.B, false.B)
            
            // should go low after 2 cycles
            dut.clock.step(2)
            checkTestOutputs(dut, false.B, false.B)
            
            // should go high again after 2 cycles
            dut.clock.step(2)
            checkTestOutputs(dut, true.B, false.B)
        }
    }

    it should "switch between GPIO and PWM output based on pwm_en" in {
        test(new GPIOModule(testMode = true)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            setDefaultInputs(dut)
            
            // config
            dut.io.gpio_direction.poke(true.B)
            dut.io.gpio_output.poke(true.B) // set gpio output high
            dut.io.pwm_div.poke(2.U)
            dut.io.duty_cycle.poke(1.U)
            dut.io.pwm_period.poke(2.U)

            // disable pwm, output should be gpio output
            dut.io.pwm_en.poke(false.B)
            dut.clock.step(1)
            checkTestOutputs(dut, true.B, false.B)

            // enable pwm, should switch to pwm output
            dut.io.pwm_en.poke(true.B)
            dut.clock.step(1)
            checkTestOutputs(dut, true.B, false.B)

            // after 2 cycles, the pwm should go high
            dut.clock.step(2)
            checkTestOutputs(dut, false.B, false.B)

            // disable pwm, should then switch to gpio output
            dut.io.pwm_en.poke(false.B)
            dut.clock.step(1)
            checkTestOutputs(dut, true.B, false.B)
        }
    }
    
    it should "correctly handle polarity inversion in PWM mode" in {
        test(new GPIOModule(testMode = true)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            setDefaultInputs(dut)
            
            // config
            dut.io.gpio_direction.poke(true.B)
            dut.io.pwm_en.poke(true.B)
            dut.io.pwm_div.poke(2.U)
            dut.io.duty_cycle.poke(1.U)
            dut.io.pwm_period.poke(2.U)
            dut.io.pwm_polarity.poke(false.B)
            
            // let pwm run for a cycle
            dut.clock.step(2)
            checkTestOutputs(dut, true.B, false.B)
            
            // invert polarity now
            dut.io.pwm_polarity.poke(true.B)
            dut.clock.step(0) // Check immediate effect
            checkTestOutputs(dut, false.B, false.B)
            
            // after another cycle it should go high
            dut.clock.step(2)
            checkTestOutputs(dut, true.B, false.B)
            
            dut.clock.step(2)
            checkTestOutputs(dut, false.B, false.B)
        }
    }
}
