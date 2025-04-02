import chisel3._

class PWMGenerator extends Module {
  val io = IO(new Bundle {
    val clk_prescaled = Input(Bool()) // prescaled clock input
    val duty_cycle = Input(UInt(8.W)) // duty cycle, value between 0 and 255
    val pwm_en = Input(Bool())        // enable signal for PWM
    val pwm_out = Output(Bool())      // pwm output
  })

  val counter = RegInit(0.U(8.W))     // counter for PWM period
  val pwm_reg = RegInit(false.B)      // register to hold PWM output

// counter increments and updates PWM output when pwm is enabled
  when(io.pwm_en) {
    when(io.clk_prescaled) {
      counter := counter + 1.U
      pwm_reg := counter < io.duty_cycle
    }
  }.otherwise {
// reset counter and PWM output when disabled
    counter := 0.U
    pwm_reg := false.B
  }

  io.pwm_out := pwm_reg
}