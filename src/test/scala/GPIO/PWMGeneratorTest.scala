import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PWMGeneratorTest extends AnyFlatSpec with ChiselScalatestTester {

  "PWMGenerator" should "produce correct output for a simple duty cycle" in {
    test(new PWMGenerator).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // disable PWM
      dut.io.pwm_en.poke(false.B)
      dut.io.duty_cycle.poke(3.U)
      dut.io.pwm_period.poke(5.U)
      dut.io.pwm_polarity.poke(false.B)
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(5)
      dut.io.pwm_out.expect(false.B)

      // enable PWM with duty_cycle=3, period=5
      dut.io.pwm_en.poke(true.B)
      // pwm_out should be low initially
      dut.io.pwm_out.expect(false.B)

      // counter = 1, pwm_out should be high
      dut.io.enable_tick.poke(true.B) 
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B) 
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1) 

      // counter =2, pwm_out should be high
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B)
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)

      // counter = 3, pwm high
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B) 
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)

      // counter = 4, pwm low
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(false.B) 
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)

      // counter = 0, pwm low
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(false.B) 
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)

      // next period
      // counter = 1, pwm high
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B) 
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)
    }
  }

  it should "handle zero duty cycle (always low)" in {
    test(new PWMGenerator) { dut =>
      //configure
      dut.io.pwm_en.poke(true.B)
      dut.io.duty_cycle.poke(0.U)
      dut.io.pwm_period.poke(4.U)
      dut.io.pwm_polarity.poke(false.B)

      for (i <- 0 until 4) { // step through whole period
        dut.io.enable_tick.poke(true.B)
        dut.clock.step(1)
        dut.io.pwm_out.expect(false.B)
        dut.io.enable_tick.poke(false.B)
        dut.clock.step(1)
      }
    }
  }

  it should "handle duty cycle equal to period (always high)" in {
    test(new PWMGenerator).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.pwm_en.poke(true.B)
      dut.io.duty_cycle.poke(4.U)
      dut.io.pwm_period.poke(4.U)
      dut.io.pwm_polarity.poke(false.B)

      // counter=0, pwm_out low
      dut.io.pwm_out.expect(false.B)

      // first step, pwm_out should go high
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B)
      dut.io.enable_tick.poke(false.B)

      // then every subsequent tick should stay high
      for (i <- 0 until 7) {
        dut.io.enable_tick.poke(true.B)
        dut.clock.step(1)
        dut.io.pwm_out.expect(true.B) 
        dut.io.enable_tick.poke(false.B)
        dut.clock.step(1)
      }
    }
  }

  it should "correctly handle polarity inversion" in {
    test(new PWMGenerator).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.pwm_en.poke(true.B)
      dut.io.duty_cycle.poke(2.U)
      dut.io.pwm_period.poke(4.U)
      dut.io.pwm_polarity.poke(false.B)

      // counter = 1, pwm high
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B)
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)

      // counter= 2, pwm high
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B)
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)

      // now try flipping polarity while pwm_out is high, should then go low
      dut.io.pwm_polarity.poke(true.B)
      dut.io.pwm_out.expect(false.B)

      // counter=3, pwm goes low but it is inverted so it should go high
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B) 
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)

      // revert polarity back to false
      dut.io.pwm_polarity.poke(false.B)
      dut.io.pwm_out.expect(false.B)
    }
  }

  it should "properly handle enable/disable transitions" in {
    test(new PWMGenerator) { dut =>
      dut.io.pwm_en.poke(true.B)
      dut.io.duty_cycle.poke(3.U)
      dut.io.pwm_period.poke(5.U)
      dut.io.pwm_polarity.poke(false.B)

      // counter = 1, pwm high
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B)
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)

      // counter = 2, pwm high
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B)

      // disble pwm mid-cycle, pwm should go low
      dut.io.pwm_en.poke(false.B)
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(false.B)

      // keep disabled for a few cycles
      dut.clock.step(1)
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(false.B)
      dut.io.enable_tick.poke(false.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(false.B)

      // then re-enabling, counter starts from 0
      dut.io.pwm_en.poke(true.B)
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(true.B)  
      dut.io.enable_tick.poke(false.B)
    }
  }

  it should "handle changing duty cycle mid-operation" in {
    test(new PWMGenerator).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.pwm_en.poke(true.B)
      dut.io.duty_cycle.poke(3.U)
      dut.io.pwm_period.poke(5.U)
      dut.io.pwm_polarity.poke(false.B)

      // run for 3 ticks (counter goes 0, 1, 2)
      for (i <- 0 until 3) {
        dut.io.enable_tick.poke(true.B)
        dut.clock.step(1)
        dut.io.pwm_out.expect(true.B)  
        dut.io.enable_tick.poke(false.B)
        dut.clock.step(1) 
      }
      

      // Change duty cycle to 1
      dut.io.duty_cycle.poke(1.U)

      // counter=3 which is greater than duty_cycle=1, so pwm should go low next tick
      dut.io.enable_tick.poke(true.B)
      dut.clock.step(1)
      dut.io.pwm_out.expect(false.B)
      dut.io.enable_tick.poke(false.B)
    }
  }
}
