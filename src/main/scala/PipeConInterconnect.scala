import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import wildcat.pipeline._
import wildcat.Util

class PipeConInterconnect(addrWidth: Int, devices: Int, addressRanges: Seq[(UInt,UInt)]) extends Module {
  val io = IO(new Bundle {
    val device = Vec(devices, Flipped(new PipeCon(addrWidth)))  // Devices (e.g., UART, SPI)
    val dmem = Flipped(new MemIO)
  })

  val rdDataReg      = RegInit(0.U(32.W))
  val ackCounter     = RegInit(0.U(16.W))
  val maxStallCycles = 20.U

  val selectedIdx     = RegInit(0.U(log2Ceil(devices).W))
  val selectedRd      = RegInit(false.B)
  val selectedWr      = RegInit(false.B)
  val selectedAddress = RegInit(0.U(addrWidth.W))
  val selectedWrData  = RegInit(0.U(32.W))
  val selectedWrMask  = RegInit(0.U(4.W))

  val sIdle :: sWaitAck :: Nil = Enum(2)
  val state = RegInit(sIdle)

  // Default device I/O
  for (i <- 0 until devices) {
    io.device(i).rd      := false.B
    io.device(i).wr      := false.B
    io.device(i).address := 0.U
    io.device(i).wrData  := 0.U
    io.device(i).wrMask  := 0.U
  }

  // Drive selected device
  io.device(selectedIdx).rd      := selectedRd
  io.device(selectedIdx).wr      := selectedWr
  io.device(selectedIdx).address := selectedAddress
  io.device(selectedIdx).wrData  := selectedWrData
  io.device(selectedIdx).wrMask  := selectedWrMask

  // Address decode (only when idle)
  when (state === sIdle) {
    for (i <- 0 until devices) {
      val (startAddr, endAddr) = addressRanges(i)
      when(io.dmem.wrEnable.reduce(_ || _) && io.dmem.wrAddress >= startAddr && io.dmem.wrAddress <= endAddr ||
          io.dmem.rdEnable && io.dmem.rdAddress >= startAddr && io.dmem.rdAddress <= endAddr) {
        selectedIdx := i.U
      }
    }
  }

  // FSM
  switch (state) {
    is (sIdle) {
      when (io.dmem.wrEnable.reduce(_ || _)) {
        selectedWr      := true.B
        selectedRd      := false.B
        selectedAddress := io.dmem.wrAddress
        selectedWrData  := io.dmem.wrData
        selectedWrMask  := io.dmem.wrEnable.asUInt
        ackCounter      := 0.U

        // Check immediately if ack is high
        when (io.device(selectedIdx).ack) {
          state := sIdle  // Directly move to sIdle if ack is already high
        } .otherwise {
          state := sWaitAck  // Wait for ack if it's not high immediately
        }

      } .elsewhen (io.dmem.rdEnable) {
        selectedWr      := false.B
        selectedRd      := true.B
        selectedAddress := io.dmem.rdAddress
        rdDataReg       := io.device(selectedIdx).rdData
      }
    }

    is (sWaitAck) {
      when (io.device(selectedIdx).ack || ackCounter >= maxStallCycles) {
        selectedWr := false.B
        ackCounter := 0.U
        state := sIdle
      } .otherwise {
        ackCounter := ackCounter + 1.U
      }
    }
  }

  // Outputs
  io.dmem.rdData := rdDataReg
  io.dmem.stall  := (state === sWaitAck)
}