
import chisel3._

class Hello extends Module {
  val out = IO(Output(UInt(8.W)))

  out := 42.U
}

object Hello extends App {
  emitVerilog(new Hello())
}
