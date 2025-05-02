import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import wildcat.pipeline._
import wildcat.Util

class PipeConInterconnect(file: String, addrWidth: Int, devices: Int) extends Module {
  val io = IO(new Bundle {
    val device = Vec(devices, Flipped(new PipeCon(addrWidth)))  // Create a vector of 2 devices (UART and SPI)
    val cpu = new PipeCon(addrWidth)
    val cpu2 = Flipped(new PipeCon(addrWidth))
  })
  val addressRanges = Seq(
    ("h00000000".U, "h0000000F".U),  // Device 0 (UART)
    ("h00000010".U, "h0000001F".U)   // Device 1 (SPI)
  )

  val (memory, start) = Util.getCode(file)
  val cpu = Module(new ThreeCats())
  val dmem = Module(new PipeConMem(memory))
  cpu.io.dmem <> dmem.io
  val imem = Module(new PipeConMemory(memory))
  imem.io.address := cpu.io.imem.address
  cpu.io.imem.data := imem.io.data
  cpu.io.imem.stall := imem.io.stall
  cpu.io.dmem.stall := false.B

  io.cpu.rdData := cpu.io.dmem.rdData
  io.cpu.ack := cpu.io.dmem.stall
  io.cpu2.address := 0.U
  io.cpu2.rd := cpu.io.dmem.rdEnable
  io.cpu2.wr := cpu.io.dmem.wrEnable.reduce(_ || _)
  io.cpu2.wrData := cpu.io.dmem.wrData
  io.cpu2.wrMask := 0.U

  for (i <- 0 until io.device.length) {
    io.device(i).rd := false.B
    io.device(i).wr := false.B
    io.device(i).address := 0.U
    io.device(i).wrData := 0.U
    io.device(i).wrMask := 0.U
  }

  val rdDataReg = RegInit(0.U(32.W))
  val stall = RegInit(false.B)

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

  when(cpu.io.dmem.wrEnable.reduce(_ || _)) {
    selected.wr := true.B
    selected.rd := false.B
    selected.address := cpu.io.dmem.wrAddress
    selected.wrData := cpu.io.dmem.wrData
    selected.wrMask := cpu.io.dmem.wrEnable.asUInt
  } .elsewhen(cpu.io.dmem.rdEnable) {
    selected.rd := true.B
    selected.wr := false.B
    selected.address := cpu.io.dmem.rdAddress
    rdDataReg := selected.rdData
  }
  

  cpu.io.dmem.stall := stall
  cpu.io.dmem.rdData := rdDataReg


}
