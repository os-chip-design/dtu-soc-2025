
import chisel3._
import chisel3.util._
import wildcat.pipeline.Functions.decode
import wildcat.pipeline._

class FakeCPUInstr extends Module {
  val io = IO(Flipped(new InstrIO()))

  val addrReg = RegInit(0.U(32.W))

  addrReg := io.address

  io.data := 0x00000013.U // nop
  io.stall := false.B

  /*
  * Program
  * 
  * 02a00313
    00000393
    00732023
  * */

  switch(addrReg) {
    is(4.U) {
      io.data := 0x02a00313.U
    }

    is(8.U) {
      io.data := 0x00000393.U
    }

    is(12.U) {
      io.data := 0x00732023.U
    }
  }
}

class FakeCPUMem extends Module {
  val io = IO(Flipped(new MemIO()))
  val v = IO(Output(UInt(32.W)))

  val singleReg = RegInit(0.U(32.W))

  when(io.wrEnable(0)) {
    singleReg := io.wrData
  }

  when(io.rdEnable) {
    io.rdData := singleReg
  }.otherwise {
    io.rdData := 0.U
  }

  v := singleReg
  io.stall := false.B
}

class CPU extends Module {
  val out = IO(Output(UInt(8.W)))
  val out_address = IO(Output(UInt(32.W)))
  val out_wrData = IO(Output(UInt(32.W)))
  val out_instr = IO(Output(UInt(32.W)))
  val out_decodedInstr = IO(Output(new DecodedInstr()))


  val cpu = Module(new ThreeCats())
  val instr = Module(new FakeCPUInstr())
  val mem = Module(new FakeCPUMem())

  cpu.io.dmem <> mem.io

  instr.io.address := cpu.io.imem.address
  out_decodedInstr <> decode(instr.io.data)
  cpu.io.imem.data := instr.io.data
  cpu.io.imem.stall := instr.io.stall

  out_address := cpu.io.imem.address
  out_wrData := cpu.io.dmem.wrData
  out_instr := instr.io.data

  out := mem.v(7, 0)
}

object CPU extends App {
  emitVerilog(new CPU(), Array("--target-dir", "generated"))
}
