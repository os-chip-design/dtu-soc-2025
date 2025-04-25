import chisel3._
import chisel3.util._

// GPIOModule that includes standard gpio functionality and pwm
// some functionality that needs to be added:
// - drive strength
// - pullup/pulldown
// - open drain
class GPIOModule extends Module {
  val io = IO(new Bundle {
    // gpio config
    val gpio_direction = Input(Bool()) // 1 = output, 0 = input
    val gpio_output = Input(Bool())    // data to output when in output mode
    val gpio_input = Output(Bool())    // data read from the pin
    
    // pad config
    val drivestrength = Input(UInt(2.W)) // drive strength configuration
    val pullup_en = Input(Bool())        // pullup enable
    val pulldown_en = Input(Bool())      // pulldown enable
    val opendrain_en = Input(Bool())     // open drain enable
    
    // pwm config
    val pwm_div = Input(UInt(8.W))       // divisor for prescaler
    val duty_cycle = Input(UInt(8.W))    // duty cycle, value between 0 and 255
    val pwm_en = Input(Bool())           // enable PWM functionality
  })
  
  // instantiate gpio pad
  val gpioPadTop = Module(new GpioPadTop)
  
  // instantiate pwm module
  val pwmTop = Module(new PWMTop)
  pwmTop.io.pwm_div := io.pwm_div
  pwmTop.io.duty_cycle := io.duty_cycle
  pwmTop.io.pwm_en := io.pwm_en
  val pwmOutput = pwmTop.io.pwm_out
  
  // connect gpio config signals to pad
  io.gpio_input := gpioPadTop.io.IN
  
  // Mux-based selection between gpio output and pwm output
  val outputData = Mux(io.pwm_en, pwmOutput, io.gpio_output)
  
  // connect to the pad
  gpioPadTop.io.OUT := outputData
  gpioPadTop.io.OE_N := !io.gpio_direction  // OE_N is active low
  gpioPadTop.io.drivestrength := io.drivestrength
  gpioPadTop.io.pullup_en := io.pullup_en
  gpioPadTop.io.pulldown_en := io.pulldown_en
  gpioPadTop.io.opendrain_en := io.opendrain_en
}