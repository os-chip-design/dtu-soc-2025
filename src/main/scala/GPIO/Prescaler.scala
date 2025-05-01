import chisel3._
import chisel3.util._

// prescaler thingy for pwm, divides clock by pwm_div

class Prescaler extends Module {
  val io = IO(new Bundle {
    val pwm_div = Input(UInt(8.W))
    val clk_prescaled = Output(Bool())
  })

// counter register, 8 bits wide
  val counter = RegInit(0.U(8.W)) // counts cycles
  val toggle = RegInit(false.B) // flips to make the output clock

// count up, flip the toggle when we hit the divisor-1
// check if counter is one less than the divisor
  when(counter === (io.pwm_div - 1.U)) {
    counter := 0.U // reset counter
    toggle := ~toggle // flip the output bit
  } .otherwise {
    counter := counter + 1.U // keep counting if not resetting
  }
  
  io.clk_prescaled := toggle
}
