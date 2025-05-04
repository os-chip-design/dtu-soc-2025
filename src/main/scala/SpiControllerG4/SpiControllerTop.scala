package SpiControllerG4

import chisel3._
import chisel3.util._

class SpiControllerTop extends Module {
  val io = IO(new Bundle {
    //Hardware IO
    val spiMiso = Input(Bool())
    val spiMosi = Output(Bool())
    val spiCs = Output(Bool())
    val spiClk = Output(Bool())

    // Register IOs
    val txReg0 = Input(UInt(32.W))
    val txReg1 = Input(UInt(32.W))
    val rxReg0 = Output(UInt(32.W))
    val rxReg1= Output(UInt(32.W))
    val controlReg = Input(UInt(32.W))
    val flagReg= Output(UInt(32.W))
  })

  val controller = Module(new SpiController)

  io.spiMosi := controller.io.spiMosi
  io.spiCs := controller.io.spiCs
  io.spiClk := controller.io.spiClk
  controller.io.spiMiso := io.spiMiso


  val enableflag = io.controlReg(31)
  val enableDone   = RegInit(false.B)
  val enablePulse  = RegInit(false.B)


  when(controller.io.ready && enableflag && !enablePulse && !enableDone){
    enablePulse := true.B
    enableDone := true.B

  }.otherwise{
    enablePulse := false.B
  }

  when(!enableflag){
    enableDone:= false.B
  }

  controller.io.txData := Cat(io.txReg1,io.txReg0)

  controller.io.receiveLength :=  io.controlReg(26,20)
  controller.io.numWaitCycles :=  io.controlReg(19,13)
  controller.io.sendLength :=     io.controlReg(12,6)
  controller.io.prescale :=       io.controlReg(5,2)
  controller.io.mode :=           io.controlReg(1,0)

  controller.io.enable := enablePulse

  val rxReg0 = RegInit(0.U(32.W))
  val rxReg1 = RegInit(0.U(32.W))
  rxReg0 := controller.io.rxData(31,0)
  rxReg1 := controller.io.rxData(63,32)

  io.rxReg0 := rxReg0
  io.rxReg1 := rxReg1


  io.flagReg := Cat(0.U(30.W),controller.io.done,controller.io.ready)  // You can update flags later if needed

}

// Add this object to generate Verilog
object SpiControllerTop extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SpiControllerTop(), args)
}
