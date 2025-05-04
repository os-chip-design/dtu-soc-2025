import chisel3._
import chisel3.util._

class PWMGenerator extends Module {
  val io = IO(new Bundle {
    val enable_tick = Input(Bool()) // incoming tick from prescaler
    val duty_cycle = Input(UInt(8.W)) // duty cycle, value between 0 and 255
    val pwm_period = Input(UInt(8.W)) // set the period of pwm
    val pwm_polarity = Input(Bool()) // polarity of the PWM signal
    val pwm_en = Input(Bool())        // enable signal for PWM
    val pwm_out = Output(Bool())      // pwm output
  })

  val counter = RegInit(0.U(8.W))
  val pwm_reg = RegInit(false.B)

  // this logic here increments a counter based on the enable_tick signal from the prescaler
  // say that pwm_div is 1, then the counter here will increment every clock cycle.
  // if pwm_div is 4, then the counter will increment every 4 clock cycles.

  when(io.pwm_en) {
    // update counter and PWM output only when enable_tick is high
    when(io.enable_tick) {
      pwm_reg := counter < io.duty_cycle

      when(counter === io.pwm_period - 1.U) {
        counter := 0.U
      }.otherwise {
        counter := counter + 1.U
      }
    }
    // pwm_reg should then hold its value when enable_tick is low
  }.otherwise {
    // reset when pwm disabled
    counter := 0.U
    pwm_reg := false.B
  }

  // apply polarity - if polarity is true, invert the output
  io.pwm_out := pwm_reg ^ io.pwm_polarity
}