import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import wildcat.pipeline._
import wildcat.Util

class PipeConInterconnect(addrWidth: Int, devices: Int, addressRanges: Seq[(UInt,UInt)]) extends Module {
  val io = IO(new Bundle {
    val device = Vec(devices, Flipped(new PipeCon(addrWidth)))  // Create a vector of 2 devices (UART and SPI)
    val dmem = Flipped(new MemIO)
  })

  val rdDataReg = RegInit(0.U(32.W))
  val stall = RegInit(false.B)
  val waitingForAck = RegInit(false.B)
  val ackCounter = RegInit(0.U(16.W)) // Enough to count up to 65k
  val maxStallCycles = 20.U

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
    when(io.dmem.wrAddress >= startAddr && io.dmem.wrAddress <= endAddr) {
      selected <> io.device(i)
    }
  }

  // Write handling: start write, then begin waiting for ack
  when(io.dmem.wrEnable.reduce(_ || _)) {
    selected.wr := true.B
    selected.rd := false.B
    selected.address := io.dmem.wrAddress
    selected.wrData := io.dmem.wrData
    selected.wrMask := io.dmem.wrEnable.asUInt
    // Check if ack is immediately high or not
    when(selected.ack) {
      waitingForAck := false.B  // If ack is already high, no need to wait
    } .otherwise {
      waitingForAck := true.B  // Otherwise, wait for ack
    }
  } .elsewhen(io.dmem.rdEnable) {
    // Read happens when not writing
    selected.rd := true.B
    selected.wr := false.B
    selected.address := io.dmem.rdAddress
    rdDataReg := selected.rdData
  }

  // Stall logic for post-write ack wait (max 20 cycles)
  when(waitingForAck || io.dmem.wrEnable.reduce(_ || _)) {
    stall := true.B
    when(selected.ack || ackCounter >= maxStallCycles) {
      waitingForAck := false.B
      stall := false.B
      ackCounter := 0.U
    } .otherwise {
      ackCounter := ackCounter + 1.U
    }
  } .otherwise {
    stall := false.B
  }

  // Output signals
  io.dmem.stall := stall
  io.dmem.rdData := rdDataReg
  

}