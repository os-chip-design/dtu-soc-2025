import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PrescalerTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "Prescaler"

    it should "toggle output at the correct rate based on pwm_div" in {
        test(new Prescaler) { dut =>
            // first reset the circuit properly
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)
            
            dut.io.clk_prescaled.expect(false.B)
            
            // test case 1: divisor = 1 
            dut.io.pwm_div.poke(1.U)
            
            dut.clock.step(1) 
            dut.io.clk_prescaled.expect(true.B) // should be true after 1 cycle
            
            dut.clock.step(1)
            dut.io.clk_prescaled.expect(false.B) // should be false after 2 cycles
            
            // test case 2: divisor = 2
            dut.io.pwm_div.poke(2.U)
            
            dut.clock.step(2)  
            dut.io.clk_prescaled.expect(true.B) // should be true after 2 cycles
            
            dut.clock.step(2)
            dut.io.clk_prescaled.expect(false.B) // should be false after 4 cycles total
            
            // test case 3: longer run with divisor = 8
            dut.reset.poke(true.B)
            dut.clock.step(1)
            dut.reset.poke(false.B)
            dut.io.clk_prescaled.expect(false.B)
            
            dut.io.pwm_div.poke(8.U)
            
            dut.clock.step(8) 
            dut.io.clk_prescaled.expect(true.B) // should be true after 8 cycles
            
            dut.clock.step(8)  
            dut.io.clk_prescaled.expect(false.B) // should be false after 16 cycles
        }
    }
}