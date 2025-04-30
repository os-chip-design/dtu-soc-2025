import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import wildcat.pipeline._
import wildcat.Util

class PipeConInterconnect(file: String, addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val uart = Flipped(new PipeCon(addrWidth))
    //val SPI  = new PipeCon(addrWidth)
  })

  val (memory, start) = Util.getCode(file)

  // Here switch between different designs
  val cpu = Module(new ThreeCats())
  // val cpu = Module(new WildFour())
  // val cpu = Module(new StandardFive())
  val dmem = Module(new PipeConMem(memory))
  cpu.io.dmem <> dmem.io
  val imem = Module(new PipeConMemory(memory))
  imem.io.address := cpu.io.imem.address
  cpu.io.imem.data := imem.io.data
  cpu.io.imem.stall := imem.io.stall
  cpu.io.dmem.stall := false.B

  io.uart.rd := false.B
  io.uart.wr := false.B
  io.uart.address := 0.U
  io.uart.wrData := 0.U
  io.uart.wrMask := 0.U

  val rdDataReg = RegInit(0.U(32.W))
  val stall = RegInit(false.B)


  val uartAddress = "h00000004".U

  when(cpu.io.dmem.wrEnable.reduce(_ || _)) {
    when(cpu.io.dmem.wrAddress === uartAddress) {
      io.uart.wr := true.B
      io.uart.rd := false.B
      io.uart.address := cpu.io.dmem.wrAddress
      io.uart.wrData := cpu.io.dmem.wrData
      io.uart.wrMask := cpu.io.dmem.wrEnable.asUInt
    }
  } .elsewhen(cpu.io.dmem.rdEnable) {
    when(cpu.io.dmem.rdAddress === uartAddress) {
      io.uart.rd := true.B
      io.uart.wr := false.B
      io.uart.address := cpu.io.dmem.rdAddress
      rdDataReg := io.uart.rdData
    } .otherwise {
      io.uart.address := cpu.io.dmem.wrAddress
      io.uart.wrData := cpu.io.dmem.wrData
      rdDataReg := io.uart.rdData
    }
  }
  
  cpu.io.dmem.stall := stall
  cpu.io.dmem.rdData := rdDataReg


}
