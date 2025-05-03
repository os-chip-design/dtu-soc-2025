import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import wildcat.pipeline._
import wildcat.Util

class PipeConInterconnect(file: String, addrWidth: Int, devices: Int, addressRanges: Seq[(UInt,UInt)]) extends Module {
  val io = IO(new Bundle {
    val device = Vec(devices, Flipped(new PipeCon(addrWidth)))  // Vector of peripheral devices
    val cpuRdAddress = Output(UInt(32.W))
    val cpuRdData = Output(UInt(32.W))
    val cpuRdEnable = Output(Bool())
    val cpuWrAddress = Output(UInt(32.W))
    val cpuWrData = Output(UInt(32.W))
    val cpuWrEnable = Output(UInt(4.W))
    val cpuStall = Output(Bool())
  })

  val (memory, start) = Util.getCode(file)
  val cpu = Module(new ThreeCats())
  val dmem = Module(new PipeConMem(memory))
  cpu.io.dmem <> dmem.io

  val imem = Module(new PipeConMemory(memory))
  imem.io.address := cpu.io.imem.address
  cpu.io.imem.data := imem.io.data
  cpu.io.imem.stall := imem.io.stall

  val rdDataReg = RegInit(0.U(32.W))
  val stall = RegInit(false.B)
  val waitingForAck = RegInit(false.B)
  val ackCounter = RegInit(0.U(16.W)) // Enough to count up to 65k
  val maxStallCycles = 2.U


  // Output signals
  cpu.io.dmem.stall := stall
  cpu.io.dmem.rdData := rdDataReg
  io.cpuStall := stall

  io.cpuRdAddress := cpu.io.dmem.rdAddress
  io.cpuRdData := cpu.io.dmem.rdData
  io.cpuRdEnable := cpu.io.dmem.rdEnable
  io.cpuWrAddress := cpu.io.dmem.wrAddress
  io.cpuWrData := cpu.io.dmem.wrData
  io.cpuWrEnable := cpu.io.dmem.wrEnable.asUInt

  // Default values for devices
  for (i <- 0 until io.device.length) {
    io.device(i).rd := false.B
    io.device(i).wr := false.B
    io.device(i).address := 0.U
    io.device(i).wrData := 0.U
    io.device(i).wrMask := 0.U
  }

  // Device selection based on address ranges
  val selected = Wire(new PipeCon(addrWidth))
  selected.rd := false.B
  selected.wr := false.B
  selected.address := 0.U
  selected.wrData := 0.U
  selected.wrMask := 0.U
  selected.rdData := 0.U
  selected.ack := false.B

  for (i <- 0 until io.device.length) {
    val (startAddr, endAddr) = addressRanges(i)
    when(cpu.io.dmem.wrAddress >= startAddr && cpu.io.dmem.wrAddress <= endAddr) {
      selected <> io.device(i)
    }
  }

  // Write handling: start write, then begin waiting for ack
  when(cpu.io.dmem.wrEnable.reduce(_ || _)) {
    selected.wr := true.B
    selected.rd := false.B
    selected.address := cpu.io.dmem.wrAddress
    selected.wrData := cpu.io.dmem.wrData
    selected.wrMask := cpu.io.dmem.wrEnable.asUInt

    waitingForAck := true.B
    ackCounter := 0.U
  } .elsewhen(cpu.io.dmem.rdEnable) {
    // Read happens when not writing
    selected.rd := true.B
    selected.wr := false.B
    selected.address := cpu.io.dmem.rdAddress
    rdDataReg := selected.rdData
  }

  // Stall logic for post-write ack wait (max 20 cycles)
  when(waitingForAck || cpu.io.dmem.wrEnable.reduce(_ || _)) {
    stall := true.B
    when(selected.ack || ackCounter >= maxStallCycles) {
      waitingForAck := false.B
      stall := false.B
    } .otherwise {
      ackCounter := ackCounter + 1.U
    }
  } .otherwise {
    stall := false.B
  }
}
