import chisel3._
import chisel3.util._

// generates tick signal every pwm_div-1 cycles
// assumes pwm_div is always >= 1.
class Prescaler extends Module {
  val io = IO(new Bundle {
    val pwm_div = Input(UInt(8.W)) 
    val enable_tick = Output(Bool()) 
  })

  val counter = RegInit(0.U(8.W))
  val tick = WireDefault(false.B)

  //tick generator every pwm_div-1 cycles
  when(counter === (io.pwm_div - 1.U)) {
    counter := 0.U
    tick := true.B
  }.otherwise {
    counter := counter + 1.U
    tick := false.B
  }

  io.enable_tick := tick
}
