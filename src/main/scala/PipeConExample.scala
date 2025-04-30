import chisel3._
import wildcat.pipeline.ThreeCats

import chisel3._

class PipeConExample(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val uartWrDataTest = Input(UInt(32.W))
    val uartRdData = Output(UInt(32.W))
    val uartRd = Input(Bool())
    val uartWr = Input(Bool())
    val uartWrMask = Input(UInt(4.W))
    val uartWrData = Input(UInt(32.W))

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
  interconnect.io.uart.wrData := io.uartWrDataTest
  interconnect.io.uart.rd := io.uartRd
  interconnect.io.uart.wr := io.uartWr
  interconnect.io.uart.wrMask := io.uartWrMask

  //interconnect.SPI.testIo.testRdData := io.SPIRdDataTest

  // Export UART outputs from interconnect
  io.uartRdData := interconnect.io.uart.rdData

  // Export SPI outputs from interconnect
  io.SPIRdData := interconnect.io.SPI.rdData
  io.SPIRd := interconnect.io.SPI.rd
  io.SPIWr := interconnect.io.SPI.wr
  io.SPIWrMask := interconnect.io.SPI.wrMask
  io.SPIWrData := interconnect.io.SPI.wrData
}
