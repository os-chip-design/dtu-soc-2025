import chisel3._
import chisel3.util._

// GPIOModule that includes standard gpio functionality and pwm
// some functionality that needs to be added:
// - pullup/pulldown
// - open drain
class GPIOModule(testMode: Boolean = false) extends Module {
  val io = IO(new Bundle {
    // gpio config
    val gpio_direction = Input(Bool()) // 1 = output, 0 = input
    val gpio_output = Input(Bool())    // data to output when in output mode
    val gpio_input = Output(Bool())    // data read from the pin
    
    // pad config
    val drivestrength = Input(UInt(2.W)) // drive strength configuration (3 bits representing 8 different levels)
    val pullup_en = Input(Bool())        // pullup enable
    val pulldown_en = Input(Bool())      // pulldown enable
    val opendrain_en = Input(Bool())     // open drain enable
    
    // pwm config
    val pwm_div = Input(UInt(8.W))       // divisor for prescaler
    val duty_cycle = Input(UInt(8.W))    // duty cycle, value between 0 and 255
    val pwm_period = Input(UInt(8.W))    // period of the PWM signal
    val pwm_polarity = Input(Bool())     // polarity of the PWM signal
    val pwm_en = Input(Bool())           // enable PWM functionality

    // test ports used when test mode is activated
    val test_PAD_IN = if (testMode) Some(Input(Bool())) else None
    val test_OUT = if (testMode) Some(Output(Bool())) else None
    val test_OE_N = if (testMode) Some(Output(Bool())) else None
  })
  
  // instantiate gpio pad
 // val gpioPadTop = Module(new GpioPadTop)
  
  // instantiate pwm module
  val pwmTop = Module(new PWMTop)
  pwmTop.io.pwm_div := io.pwm_div
  pwmTop.io.duty_cycle := io.duty_cycle
  pwmTop.io.pwm_period := io.pwm_period
  pwmTop.io.pwm_polarity := io.pwm_polarity
  pwmTop.io.pwm_en := io.pwm_en
  val pwmOutput = pwmTop.io.pwm_out
  
  // connect gpio config signals to pad
  // io.gpio_input := gpioPadTop.io.IN
  
  // Mux-based selection between gpio output and pwm output
  val outputData = Mux(io.pwm_en, pwmOutput, io.gpio_output)

  val outputEnableN = !io.gpio_direction // OE_N is active low

  if (testMode) {
    // test mode: connect directly to test ports
    io.gpio_input := io.test_PAD_IN.get
    io.test_OUT.get := outputData
    io.test_OE_N.get := outputEnableN
  } else {
    // normal mode: connect to GPIOPad
    val gpioPadTop = Module(new GpioPadTop)

    // connect gpio config signals to pad
    io.gpio_input := gpioPadTop.io.IN
  
    // connect to the pad
    gpioPadTop.io.OUT := outputData
    gpioPadTop.io.OE_N := outputEnableN  // OE_N is active low
    gpioPadTop.io.drivestrength := io.drivestrength
    gpioPadTop.io.pullup_en := io.pullup_en
    gpioPadTop.io.pulldown_en := io.pulldown_en
    gpioPadTop.io.opendrain_en := io.opendrain_en
  }
}