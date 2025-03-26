import chisel3._
import chisel3.util._

// implement a prescaler module to be used in a PWM that divides the input clock by a given divisor

class Prescaler(divisor: Int) extends Module {
  require(divisor > 0, "Divisor must be greater than 0")

  val io = IO(new Bundle {
    val clk_out = Output(Bool())
  })

// initalize counter with an 8-bit register
  val counter = RegInit(0.U(8.W)) // counter signal 
  val toggle = RegInit(false.B) // toggle signal used to generate the scaled clock signal

// increment counter and toggle output when divisor is reached
  counter := counter + 1.U
  when(counter === (divisor - 1).U) {
    counter := 0.U
    toggle := ~toggle
  }

  io.clk_out := toggle
}
