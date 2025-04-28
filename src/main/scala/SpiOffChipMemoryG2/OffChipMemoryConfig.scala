import chisel3._
import chisel3.util._

class OffChipMemoryConfig() extends Module {
  val pipeCon = IO(new PipeCon(24))
  val config = IO(new configIO)

  val configRegister = RegInit(0.U(32.W))

  config.jedec := configRegister(0) // Indicates if the JEDEC ID should be read upon a read request (instead of reading the data)
  config.clear := configRegister(1) // Indicates if the memory should be cleared upon a write request (instead of writing the data)
  config.clockDivision := configRegister(30, 2) // Clock division factor for the SPI clock (subtracted by 1 so it should not be 0)
  config.mode := configRegister(31) // SPI clock mode, 0 (indicated by 0) or 3 (indicated by 1)
 
  object State extends ChiselEnum {
    val idle, finishWrite, finishRead = Value
  }

  val stateReg = RegInit(State.idle)
  pipeCon.rdData := configRegister

  when (pipeCon.wr) {
    configRegister := pipeCon.wrData
  }
  when (pipeCon.rd) {
    stateReg := State.finishRead
  }.elsewhen (pipeCon.wr) {
    stateReg := State.finishWrite
  }

  switch(stateReg) {
    is(State.finishRead) {
      pipeCon.ack := true.B
      pipeCon.rdData := configRegister
      stateReg := State.idle
    }

    is(State.finishWrite) {
      pipeCon.ack := true.B
      stateReg := State.idle
    }
  } 
}
