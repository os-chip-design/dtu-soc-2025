import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PrescalerTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "Prescaler"

    it should "generate enable tick at the correct rate based on pwm_div" in { 
        test(new Prescaler).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
            // reset
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)
            // should be false initially
            dut.io.enable_tick.expect(false.B)

            // test case 1: divisor = 1, output should be high every cycle
            dut.io.pwm_div.poke(1.U)
            
            dut.io.enable_tick.expect(true.B)
            dut.clock.step(1)
            dut.io.enable_tick.expect(true.B)
            dut.clock.step(1)
            dut.io.enable_tick.expect(true.B)
            dut.clock.step(1)
            dut.io.enable_tick.expect(true.B)
            dut.clock.step(1)
            dut.io.enable_tick.expect(true.B)

            // test case 2: divisor = 2, output should flip every 2 cycles
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)
            dut.io.pwm_div.poke(2.U)
        
            dut.io.enable_tick.expect(false.B)
            dut.clock.step(1) 
            dut.io.enable_tick.expect(true.B) 
            dut.clock.step(1) 
            dut.io.enable_tick.expect(false.B)
            dut.clock.step(1) 
            dut.io.enable_tick.expect(true.B) 
            dut.clock.step(1) 
            dut.io.enable_tick.expect(false.B)
            dut.clock.step(1) 
            dut.io.enable_tick.expect(true.B)
            dut.clock.step(1) 
            dut.io.enable_tick.expect(false.B)
            dut.clock.step(1) 
            dut.io.enable_tick.expect(true.B)

            // test case 3: longer run with divisor = 8
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)
            dut.io.pwm_div.poke(8.U)
            dut.io.enable_tick.expect(false.B)

            //counter = 0

            // step 6 times, counter goes 0 to 6
            dut.clock.step(6)
            dut.io.enable_tick.expect(false.B) 

            // step 1 more time, counter will be 7
            dut.clock.step(1)
            dut.io.enable_tick.expect(true.B) 

            // step 1 more time, counter becomes 0
            dut.clock.step(1)
            dut.io.enable_tick.expect(false.B)

            // step 6 more times, counter goes 0 to 6
            dut.clock.step(6)
            dut.io.enable_tick.expect(false.B) 

            // step 1 more time, counter becomes 7
            dut.clock.step(1)
            dut.io.enable_tick.expect(true.B) 

            // step 1 more time, counter becomes 0
            dut.clock.step(1)
            dut.io.enable_tick.expect(false.B) 
        }
    }
}