import wildcat.pipeline._

import chisel3._

class TopLevelWithThreeCats extends Module {
  val io = IO(new Bundle {
    val dmemRdAddress = Output(UInt(32.W))    // From CPU to data memory
    val dmemRdEnable = Output(Bool())         // From CPU to data memory
    val dmemRdData = Input(UInt(32.W))        // From data memory to CPU
    val dmemWrAddress = Output(UInt(32.W))    // From CPU to data memory
    val dmemWrData = Output(UInt(32.W))       // From CPU to data memory
    val dmemWrEnable = Output(Vec(4, Bool())) // From CPU to data memory
    val dmemStall = Input(Bool())             // From memory to CPU
  })

  val cpu = Module(new ThreeCats())

  // Instruction memory connections
  cpu.io.imem.data := 0.U
  cpu.io.imem.stall := false.B

  // Data memory connections
  io.dmemRdAddress := cpu.io.dmem.rdAddress
  io.dmemRdEnable := cpu.io.dmem.rdEnable
  cpu.io.dmem.rdData := io.dmemRdData

  io.dmemWrAddress := cpu.io.dmem.wrAddress
  io.dmemWrData := cpu.io.dmem.wrData
  io.dmemWrEnable := cpu.io.dmem.wrEnable
  cpu.io.dmem.stall := io.dmemStall




}
