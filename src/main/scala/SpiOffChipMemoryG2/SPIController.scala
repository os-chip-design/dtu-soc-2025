import chisel3._
import chisel3.util._


class SPIController(
    val spiFreq: Int = 1000000,
    val freq: Int = 50000000,
    val addrWidth: Int = 24,
    val dataWidth: Int = 32,
) extends Module {
  val spiPort = IO(new spiIO)

  val interconnectPort = IO(new Bundle {
    val valid = Input(Bool())
    val ready = Input(Bool())
    val currInstr = Input(UInt(2.W))
    val dataIn = Input(UInt(dataWidth.W))
    val address = Input(UInt(addrWidth.W))
    val dataOut = Output(UInt(dataWidth.W))
    val done = Output(Bool())
  })

  object State extends ChiselEnum {
    val idle, spiInstrTransmit, sendAddress,
        receiveData, writeData, finished= Value        
  }

  def risingEdge(x: Bool) = x && !RegNext(x)

  // Registers
  val stateReg = RegInit(State.idle)
  val dataOutReg = RegInit(VecInit(Seq.fill(dataWidth)(0.U(1.W))))
  val pointerReg = RegInit(0.U(32.W))
  
  //  SPI clock counter 
  val spiClkCounterReg = RegInit(0.U(32.W))
  val spiClkCounterMax = ((freq / spiFreq / 2) - 1).U 
  val spiClkReg = RegInit(false.B)
  val risingEdgeOfSPIClk = risingEdge(spiClkReg)

  // Instructions
  val readJEDECInstruction   =  "b10011111".U // 0x9F (Read JeDEC ID), table 8.1.3
  val writeEnableInstruction =  "b00000110".U // 0x06 (Write Enable), table 8.13
  val pageProgramInstruction =  "b00000010".U // 0x02 (Page Program), table 8.1.3
  val readDataInstruction    =  "b00000011".U // 0x03 (Read Data), table 8.1.3

  val instructions = VecInit(Seq.fill(4)(0.U(8.W))) // 4 instructions for the flash memory
  instructions(0) := readJEDECInstruction
  instructions(1) := writeEnableInstruction
  instructions(2) := pageProgramInstruction
  instructions(3) := readDataInstruction

  val rdData = dataOutReg.reverse.reduce(_ ## _)

  val data = interconnectPort.dataIn // Data to be sent to the flash memory
  val address = interconnectPort.address // Address to be sent to the flash memory
  val instruction = instructions(interconnectPort.currInstr) // Select the instruction to be sent to the flash memory

  spiPort.dataOut          := 0.U
  spiPort.spiClk           := spiClkReg
  spiPort.chipSelect       := true.B
  interconnectPort.done    := false.B
  interconnectPort.dataOut := rdData

  when(spiClkCounterReg === spiClkCounterMax) {
    spiClkCounterReg := 0.U
    spiClkReg := !spiClkReg
  }.otherwise {
    spiClkCounterReg := spiClkCounterReg + 1.U
  }

  switch(stateReg) {

    // --- starting ---
    is(State.idle) {
      when (interconnectPort.valid) {
        spiPort.chipSelect := false.B
        stateReg := State.spiInstrTransmit
        pointerReg := 7.U

        dataOutReg := VecInit(Seq.fill(dataWidth)(0.U(1.W))) // reset the dataOut register
      }
    }
    
    // -- sending the command --
    is(State.spiInstrTransmit) {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          when(instruction === readJEDECInstruction){  // ReadJeDECInstruction
            stateReg := State.receiveData
            pointerReg := 23.U
          }.elsewhen(instruction === writeEnableInstruction){ // WriteEnableInstruction
            stateReg := State.finished
          }.otherwise{ 
            stateReg := State.sendAddress
            pointerReg := (addrWidth - 1).U 
          }
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }
      spiPort.dataOut := instruction(pointerReg)
    }
    
    is (State.sendAddress) {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          when(instruction === pageProgramInstruction){
            stateReg := State.writeData
            pointerReg := (dataWidth - 1).U
          }.elsewhen(instruction === readDataInstruction){
            stateReg := State.receiveData
            pointerReg := (dataWidth - 1).U
          }.otherwise {
            stateReg := State.receiveData
            pointerReg := 7.U
          }
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }
      spiPort.dataOut := address(pointerReg)
    }

    is (State.writeData)
    {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          stateReg := State.finished
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }
      spiPort.dataOut := data(pointerReg)
    }

    // -- obtaining the data from the device --
    is (State.receiveData)
    {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          stateReg := State.finished
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }
      dataOutReg(pointerReg) := spiPort.dataIn
    }

    is (State.finished)
    {
      when(interconnectPort.ready) {
        stateReg := State.idle
      }

      interconnectPort.done := true.B
    }
  }
}
