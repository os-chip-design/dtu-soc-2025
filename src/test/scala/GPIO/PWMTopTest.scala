import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PWMTopTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "PWMTop"

    it should "produce correct PWM signal for basic parameters" in {
        test(new PWMTop).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            // intialize and reset
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)

            // configuration
            dut.io.pwm_div.poke(2.U) 
            dut.io.duty_cycle.poke(3.U) 
            dut.io.pwm_period.poke(6.U) 
            dut.io.pwm_polarity.poke(false.B)
            dut.io.pwm_en.poke(false.B)
            
            // output should be low when pwm is disabled
            dut.clock.step(5)
            dut.io.pwm_out.expect(false.B)

            // enable pwm
            dut.io.pwm_en.poke(true.B)
            dut.io.pwm_out.expect(false.B)
            
            // counter = 1, output should be high
            dut.clock.step(2)
            dut.io.pwm_out.expect(true.B)
            dut.io.pwm_out.peek()
            
            // counter = 2, output remains high
            dut.clock.step(2)
            dut.io.pwm_out.expect(true.B)
            dut.io.pwm_out.peek()
            
            // counter = 3, output remains high
            dut.clock.step(2)
            dut.io.pwm_out.expect(true.B)
            dut.io.pwm_out.peek()
            
            // counter = 4, output goes low
            dut.clock.step(2)
            dut.io.pwm_out.expect(false.B)
            dut.io.pwm_out.peek()

            // counter = 5, output remains low
            dut.clock.step(2)
            dut.io.pwm_out.expect(false.B)
            dut.io.pwm_out.peek()

            // counter = 0 (reset), output remains low
            dut.clock.step(2)
            dut.io.pwm_out.expect(false.B)
            dut.io.pwm_out.peek()

            // counter = 1, output goes high
            dut.clock.step(2)
            dut.io.pwm_out.expect(true.B)
            dut.io.pwm_out.peek()

            // counter = 2, output remains high
            dut.clock.step(2)
            dut.io.pwm_out.expect(true.B)
            dut.io.pwm_out.peek()

            dut.io.pwm_polarity.poke(true.B) 
            dut.io.pwm_out.expect(false.B)

            // counter = 3, output remains low
            dut.clock.step(2)
            dut.io.pwm_out.expect(false.B)
            dut.io.pwm_out.peek()

            // counter = 4, output goes high because of polarity
            dut.clock.step(2)
            dut.io.pwm_out.expect(true.B)
            dut.io.pwm_out.peek()

            // pwm disable
            dut.io.pwm_en.poke(false.B)
            dut.clock.step(1)
            dut.io.pwm_out.expect(true.B)
            
            // restore polarity
            dut.io.pwm_polarity.poke(false.B)
            dut.io.pwm_out.expect(false.B)
        }
    }

    it should "correctly handle duty cycle greater than period" in {
        test(new PWMTop).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)

            // configuration
            dut.io.pwm_div.poke(2.U)
            dut.io.duty_cycle.poke(8.U)
            dut.io.pwm_period.poke(5.U)
            dut.io.pwm_polarity.poke(false.B)
            dut.io.pwm_en.poke(true.B)
            dut.clock.step(2)

            // output should always be high since the duty cycle is greater than period
            for (i <- 0 until 20) {
                dut.clock.step(1)
                dut.io.pwm_out.expect(true.B)
            }
        }
    }

    it should "handle rapid changes to duty cycle" in {
        test(new PWMTop).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)

            // configuration: div=2, duty=4, period=8
            dut.io.pwm_div.poke(2.U)
            dut.io.duty_cycle.poke(4.U)
            dut.io.pwm_period.poke(8.U)
            dut.io.pwm_polarity.poke(false.B)
            dut.io.pwm_en.poke(true.B)
            dut.io.pwm_out.expect(false.B) // Initial output

            // run for 3 ticks
            // should be low for the first tick
            dut.clock.step(1)
            dut.io.pwm_out.expect(false.B)

            for (i <- 0 until 3) {
                dut.clock.step(1)
                dut.io.pwm_out.expect(true.B) 
                dut.clock.step(1)
                dut.io.pwm_out.expect(true.B)
            }

            // change duty cycle to 1
            dut.io.duty_cycle.poke(1.U)

            // output should still be high, but should change to low on the next tick
            dut.io.pwm_out.expect(true.B)

            // run for 3 more ticks
            // output should be low because counter >= 1.
            for (i <- 0 until 3) {
                dut.clock.step(1)
                dut.io.pwm_out.expect(false.B)
                dut.clock.step(1)
                dut.io.pwm_out.expect(false.B) 
            }

            // change duty cycle to 7
            dut.io.duty_cycle.poke(7.U)
            dut.io.pwm_out.expect(false.B)

            dut.clock.step(1)
            dut.io.pwm_out.expect(true.B)
            dut.clock.step(1)
            dut.io.pwm_out.expect(true.B)

            // counter = 0, output goes low
            dut.clock.step(1)
            dut.io.pwm_out.expect(false.B)
            dut.clock.step(1)
            dut.io.pwm_out.expect(false.B)

            // next 7 ticks should be high
            for (i <- 0 until 7) {
                dut.clock.step(1) 
                dut.io.pwm_out.expect(true.B) 
                dut.clock.step(1) 
                dut.io.pwm_out.expect(true.B) 
            }
        }
    }

    
    it should "handle large values properly" in {
        test(new PWMTop).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)

            // test config with large values near 8-bit limit
            // more of a visual test here, vcd file should show the expected behavior. I used GTKwave for that
            dut.io.pwm_div.poke(2.U)
            dut.io.duty_cycle.poke(200.U)
            dut.io.pwm_period.poke(250.U)
            dut.io.pwm_polarity.poke(false.B)
            dut.io.pwm_en.poke(true.B)
            
            dut.clock.step(1000)
        }
    }

    it should "recover properly from reset during operation" in {
        test(new PWMTop).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            // start normally
            dut.reset.poke(false.B)
            dut.io.pwm_div.poke(2.U)
            dut.io.duty_cycle.poke(3.U)
            dut.io.pwm_period.poke(5.U)
            dut.io.pwm_polarity.poke(false.B)
            dut.io.pwm_en.poke(true.B)
            
            // let it run
            dut.clock.step(2)
            dut.io.pwm_out.expect(true.B)
            dut.clock.step(2)
            dut.io.pwm_out.expect(true.B)
            dut.clock.step(2)
            
            // apply reset mid operation
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.io.pwm_out.expect(false.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)
            dut.clock.step(1)
            
            // check if it behaves as expected after reset
            for(i <- 0 until 6) {
                dut.clock.step(1)
                dut.io.pwm_out.expect(true.B)
            }
            dut.clock.step(1)
            dut.io.pwm_out.expect(false.B)
        }
    }

    it should "handle rapid enable/disable transitions" in {
        test(new PWMTop) { dut =>
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)

            dut.io.pwm_div.poke(2.U)
            dut.io.duty_cycle.poke(3.U)
            dut.io.pwm_period.poke(5.U)
            dut.io.pwm_polarity.poke(false.B)
            
            // Toggle enable rapidly
            dut.io.pwm_en.poke(true.B)
            dut.clock.step(2)
            dut.io.pwm_en.poke(false.B)
            dut.clock.step(1)
            dut.io.pwm_out.peek()
            
            dut.io.pwm_en.poke(true.B)
            dut.clock.step(3)
            dut.io.pwm_en.poke(false.B)
            dut.clock.step(1)
            dut.io.pwm_out.peek()
            
            dut.io.pwm_en.poke(true.B)
            dut.clock.step(5)
            dut.io.pwm_en.poke(false.B)
            dut.clock.step(1)
            dut.io.pwm_out.peek()
        }
    }

    it should "produce no output when duty_cycle = 0" in {
        test(new PWMTop).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)

            // set duty cycle to 0
            dut.io.pwm_en.poke(true.B)
            dut.io.duty_cycle.poke(0.U)
            dut.io.pwm_period.poke(8.U)
            dut.io.pwm_div.poke(2.U)
            dut.io.pwm_polarity.poke(false.B)

            // output should always be low
            for (cycle <- 0 until 16) {
                dut.clock.step(1)
                dut.io.pwm_out.expect(false.B)
            }
        }
    }

    it should "produce continuous output (always high) when duty_cycle == period" in {
        test(new PWMTop).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)

            // set duty cycle the same as period
            dut.io.pwm_en.poke(true.B)
            dut.io.duty_cycle.poke(5.U)
            dut.io.pwm_period.poke(5.U)
            dut.io.pwm_div.poke(2.U)
            dut.io.pwm_polarity.poke(false.B)
            dut.clock.step(1)

            // output should always be high
            for (cycle <- 0 until 10) {
                dut.clock.step(1)
                dut.io.pwm_out.expect(true.B)
            }
            // check if output flips if the polarity is flipped
            dut.io.pwm_polarity.poke(true.B)
            for (cycle <- 0 until 10) {
                dut.clock.step(1)
                dut.io.pwm_out.expect(false.B)
            }
        }
    }

    it should "change frequency when prescaler changes mid-run" in {
        test(new PWMTop).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)

            dut.io.pwm_en.poke(true.B)
            dut.io.duty_cycle.poke(2.U)
            dut.io.pwm_period.poke(4.U)
            dut.io.pwm_div.poke(2.U)
            dut.io.pwm_polarity.poke(false.B)

            // set prescaler=2
            // should be low for first two steps
            dut.io.pwm_out.expect(false.B)
            dut.clock.step(1)
            dut.io.pwm_out.expect(false.B)
            dut.clock.step(1)

            // should be high for next four steps
            for (i <- 0 until 4) {
                dut.io.pwm_out.expect(true.B)
                dut.clock.step(1)
            }
            // should be low for next two steps
            dut.clock.step(1)
            dut.io.pwm_out.expect(false.B)
            dut.clock.step(1)
            dut.io.pwm_out.expect(false.B)

            // Now change prescaler to 4
            dut.io.pwm_div.poke(4.U)
            //should be low for first 4 steps
            for(i <- 0 until 4) {
                dut.io.pwm_out.expect(false.B)
                dut.clock.step(1)
            }
            // should be high for next 8 steps
            for(i <- 0 until 8) {
                dut.io.pwm_out.expect(true.B)
                dut.clock.step(1)
            }
            // should be low for next 4 steps
            for(i <- 0 until 4) {
                dut.io.pwm_out.expect(false.B)
                dut.clock.step(1)
            }
            
        }
    }
}
