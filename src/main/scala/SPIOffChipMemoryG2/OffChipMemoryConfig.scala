import chisel3._
import chisel3.util._

class OffChipMemoryConfig() extends Module {
  val pipeCon = IO(new PipeCon(1))
  val config = IO(Flipped(new configIO(10)))

  val defaultJEDEC = "b0".U
  val defaultClear = "b0".U
  val defaultTargetFlash = "b1".U
  val defaultClockDivision = "b0000000100".U // Default clock division is 4 (10 bits)
  val defaultMode = "b0".U // SPI clock mode, 0 (indicated by 0) or 3 (indicated by 1)
  val widthOfRest = 32 - 10 - 1 - 1 - 1 // 32 bits - clock division (10 bits) - target flash (1 bit) - clear (1 bit) - JEDEC (1 bit)
  val rest = 0.U(widthOfRest.W) // Rest of the configuration register (0 by default)
  val defaultConfig = Cat(defaultMode, rest, defaultClockDivision, defaultTargetFlash, defaultClear, defaultJEDEC) // 32 bits
  val configRegister = RegInit(defaultConfig) // 32 bits register to store the configuration

  config.jedec := configRegister(0) // Indicates if the JEDEC ID should be read upon a read request (instead of reading the data)
  config.clear := configRegister(1) // Indicates if the memory should be cleared upon a write request (instead of writing the data)
  config.targetFlash := configRegister(2) // Indicates if the target is the flash memory (instead of the RAM)
  config.clockDivision := configRegister(13, 3) // Clock division for the SPI controller (10 bits)
  config.mode := configRegister(31) // SPI clock mode, 0 (indicated by 0) or 3 (indicated by 1)
 
  object State extends ChiselEnum {
    val idle, finishWrite, finishRead = Value
  }

  val stateReg = RegInit(State.idle)
  pipeCon.rdData := configRegister
  pipeCon.ack := false.B

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
      stateReg := State.idle
    }

    is(State.finishWrite) {
      pipeCon.ack := true.B
      stateReg := State.idle
    }
  } 
}
