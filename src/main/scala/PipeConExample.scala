import chisel3._
import wildcat.pipeline.ThreeCats

import chisel3._

class PipeConExample(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val uartRdDataTest = Input(UInt(32.W))
    val uartRdData = Output(UInt(32.W))
    val uartRd = Input(Bool())
    val uartWr = Output(Bool())
    val uartWrMask = Output(UInt(4.W))
    val uartWrData = Output(UInt(32.W))

    val SPIRdDataTest = Input(UInt(32.W))
    val SPIRdData = Output(UInt(32.W))
    val SPIRd = Output(Bool())
    val SPIWr = Output(Bool())
    val SPIWrMask = Output(UInt(4.W))
    val SPIWrData = Output(UInt(32.W))
  })

  val interconnect = Module(new PipeConInterconnect(addrWidth))

  // Default assignments
  interconnect.io.uart.wrMask := 0.U
  interconnect.io.uart.wrData := 0.U
  interconnect.io.uart.address := 0.U
  interconnect.io.uart.rd := false.B
  interconnect.io.uart.wr := false.B
  interconnect.io.SPI.wrMask := 0.U
  interconnect.io.SPI.wrData := 0.U
  interconnect.io.SPI.address := 0.U
  interconnect.io.SPI.rd := false.B
  interconnect.io.SPI.wr := false.B


  // Drive test data to peripherals via interconnect
  //interconnect.uart.testIo.testRdData := io.uartRdDataTest
  //interconnect.SPI.testIo.testRdData := io.SPIRdDataTest

  // Export UART outputs from interconnect
  io.uartRdData := interconnect.io.uart.rdData
  io.uartWr := interconnect.io.uart.wr
  io.uartWrMask := interconnect.io.uart.wrMask
  io.uartWrData := interconnect.io.uart.wrData

  // Export SPI outputs from interconnect
  io.SPIRdData := interconnect.io.SPI.rdData
  io.SPIRd := interconnect.io.SPI.rd
  io.SPIWr := interconnect.io.SPI.wr
  io.SPIWrMask := interconnect.io.SPI.wrMask
  io.SPIWrData := interconnect.io.SPI.wrData
}
