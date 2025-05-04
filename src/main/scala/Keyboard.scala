/*
 * Blinking LED: the hardware version of Hello World
 *
 * Copyright: 2013, Technical University of Denmark, DTU Compute
 * Author: Martin Schoeberl (martin@jopdesign.com)
 * 
 */

import chisel3._
import chisel3.util._


class Keyboard(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    // *** Keyboard Input ***
    val data = Input(Bool())
    val step = Input(Bool())

    // *** BUS *** From PipeCon.scala
    val address = Input(UInt(addrWidth.W))
    val rd = Input(Bool())
    val wr = Input(Bool())
    val rdData = Output(UInt(32.W))
    val wrData = Input(UInt(32.W))
    val wrMask = Input(UInt(4.W))
    val ack = Output(Bool())

    // *** Debug ***
    val dataOut = Output(UInt(11.W))
    // val state = Output(UInt(3.W))
    val error = Output(Bool())

  })
  
  // Initialization
  // Testbench delay 
  val DELAY_MIN = 5.U    // 5 clock step delay
  val DELAY_MAX = 40.U   // 50 clock step delay  val DELAY_MIN = 500.U

  // FPGA delay
  // val DELAY_MIN = 500.U    // 5us delay @ 100MHz
  // val DELAY_MAX = 3500.U   // 40us delay @ 100MHz

  // Functions
  def rising(v: Bool) = v & !RegNext(v)
  def falling(v: Bool) = !v & RegNext(v)

  // Modules
  val buffer = Module(new Buffer())
  io.rdData := buffer.io.dataOut
  // io.valid := buffer.io.validOut
  buffer.io.request := io.rd

  // IO init
  io.ack := DontCare
  io.dataOut := 0.U
  io.error := 0.B
  
  //  Registers
  val bitCount = RegInit(0.U(4.W))
  val inputReg = RegInit(VecInit(Seq.fill(11)(0.B)))
  val dataReg = RegInit(0.U(11.W))
  val dataSend = RegInit(0.U(8.W))
  val error = RegInit(0.B)
  val isOdd = RegInit(0.B)
  val pairity = RegInit(0.B)
  val delayCnt = RegInit(0.U(16.W))
  val idleCnt = RegInit(0.U(16.W))
  val canRead = RegInit(0.B)
  val valid = RegInit(0.B)

  // *** Datapath ***
  buffer.io.valid := valid                
  buffer.io.dataIn := dataSend
  
  when(rising(canRead)) {
    inputReg(bitCount) := io.data
    bitCount := bitCount + 1.U
  }

  // Error checking
  //  Start bit   0  - First bit always 0 
  //  End bit     1  - Last bit always 1
  //  Odd pairity    - 8 databits + pairity bit added up needs to be odd
  isOdd := dataReg(1) + dataReg(2) + dataReg(3) + dataReg(4) + dataReg(5) + dataReg(6) + dataReg(7) + dataReg(8)
  pairity := dataReg(9)
  
  when(!dataReg(0) && dataReg(10)){
    error := 0.B
    when (!isOdd && pairity){error := 0.B}
      .elsewhen (isOdd && !pairity){error := 0.B} 
      .otherwise {error := 1.B}
  }.otherwise {error := 1.B}


  // *** FSM ***
  object State extends ChiselEnum {
    val IDLE, READ, READIDLE, FINALIZE, SEND = Value
  }

  import State._
  val stateReg = RegInit(IDLE)

  switch(stateReg){
    is (IDLE){
      canRead := 0.U
      delayCnt := 0.U
      
      // I/O
      dataSend := dataReg(8,1)
      
      io.error := error
      io.dataOut := dataReg

      when (!io.step){
        stateReg := READ
      }
    }

    is (READ){
      delayCnt := delayCnt + 1.U // Delay for reading window
      idleCnt := 0.U
      valid := 0.B
      when (delayCnt >= DELAY_MIN && delayCnt <= DELAY_MAX) {canRead := 1.B}
      .elsewhen (delayCnt > DELAY_MAX) {stateReg := READIDLE}
      }

    is (READIDLE){
      canRead := 0.U
      delayCnt := 0.U

      when (idleCnt > DELAY_MAX){
        when (bitCount >= 11.U && io.step){stateReg := FINALIZE}
        .elsewhen (!io.step){stateReg := READ}
      } .otherwise {idleCnt := idleCnt + 1.U}
    }

    is (FINALIZE){
      bitCount := 0.U
      dataReg := Cat(inputReg(10), inputReg(9), inputReg(8), inputReg(7), inputReg(6), inputReg(5), inputReg(4), inputReg(3), inputReg(2), inputReg(1), inputReg(0))
      when (delayCnt >= DELAY_MIN) {
        when (buffer.io.ready && !error) {stateReg := SEND}
        .elsewhen (buffer.io.ready && error) {stateReg := IDLE}
      } .otherwise {delayCnt := delayCnt + 1.U}
    }

    is (SEND) {
      valid := 1.B
      delayCnt := 0.U
      dataSend := dataReg(8,1)

      io.dataOut := dataReg
      when (falling(buffer.io.ready)) {stateReg := IDLE}
    }
  }
  
  when(!(stateReg === IDLE || stateReg === READ || stateReg === READIDLE || stateReg === FINALIZE || stateReg === SEND)) {stateReg := IDLE}
}
  

/**
 * An object extending App to generate the Verilog code.
 */
object KeyboardMain extends App {
  println("Hello World, I will now generate the Verilog file!")
  emitVerilog(new Keyboard(32))
}
