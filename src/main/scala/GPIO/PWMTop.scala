import chisel3._
import chisel3.util._

class PWMTop extends Module {
    val io = IO(new Bundle {
        val pwm_div = Input(UInt(8.W)) // divisor for prescaler
        val duty_cycle = Input(UInt(8.W)) // duty cycle, value between 0 and 255
        val pwm_period = Input(UInt(8.W)) // set the period of pwm
        val pwm_polarity = Input(Bool()) // add polarity input
        val pwm_en = Input(Bool()) // enable signal
        val pwm_out = Output(Bool()) // pwm output
    })

    val prescaler = Module(new Prescaler)

    val pwmGenerator = Module(new PWMGenerator)

    // prescaler input
    prescaler.io.pwm_div := io.pwm_div

    // pwm generator inputs
    pwmGenerator.io.clk_prescaled := prescaler.io.clk_prescaled
    pwmGenerator.io.duty_cycle := io.duty_cycle
    pwmGenerator.io.pwm_en := io.pwm_en
    pwmGenerator.io.pwm_period := io.pwm_period
    pwmGenerator.io.pwm_polarity := io.pwm_polarity
    
    // pwm generator output is the top module pwm output
    io.pwm_out := pwmGenerator.io.pwm_out
}