import chisel3._
import chisel3.util._




class SPIController(
) extends Module {
  val spiPort = IO(new spiIO)

  val interconnectPort = IO(new Bundle {
    val valid = Input(Bool())
    val ready = Input(Bool())
    val instruction = Input(UInt(8.W))
    val dataIn = Input(UInt(32.W))
    val address = Input(UInt(24.W))
    val dataOut = Output(UInt(32.W))
    val done = Output(Bool())
    val clockDivision = Input(UInt(32.W))
  })

  object State extends ChiselEnum {
    val idle, instructionTransmit, sendAddress,
        receiveData, writeData, finished= Value        
  }

  def risingEdge(x: Bool) = x && !RegNext(x)
  def fallingEdge(x: Bool) = !x && RegNext(x)

  // Registers
  val stateReg = RegInit(State.idle)
  val dataOutReg = RegInit(VecInit(Seq.fill(32)(0.U(1.W))))
  val pointerReg = RegInit(0.U(32.W))
  
  //  SPI clock counter 
  val spiClkCounterReg = RegInit(0.U(32.W))
  val spiClkCounterMax = interconnectPort.clockDivision - 1.U 
  val spiClkReg = RegInit(false.B)
  val risingEdgeOfSPIClk = risingEdge(spiClkReg)
  val fallingEdgeOfSPIClk = fallingEdge(spiClkReg)

  val rdData = dataOutReg.reduce(_ ## _)

  val instruction = interconnectPort.instruction // Instruction to be sent to the flash memory

  val data = interconnectPort.dataIn // Data to be sent to the flash memory
  val address = interconnectPort.address // Address to be sent to the flash memory

  val currentOutputReg = RegInit(0.U(1.W)) // Current output register for the SPI communication
  
  spiPort.dataOut          := currentOutputReg
  spiPort.chipSelect       := true.B
  spiPort.spiClk           := spiClkReg
  interconnectPort.done    := false.B
  interconnectPort.dataOut := rdData

  when(spiClkCounterReg === spiClkCounterMax) {
    spiClkCounterReg := 0.U
    spiClkReg := !spiClkReg
  }.otherwise {
    spiClkCounterReg := spiClkCounterReg + 1.U
  }

  switch(stateReg) {

    is(State.idle) {
      when (interconnectPort.valid) {
        spiPort.chipSelect := false.B
        spiPort.spiClk := false.B
        spiClkReg := false.B
        stateReg := State.instructionTransmit
        pointerReg := 7.U
      }

      when (fallingEdgeOfSPIClk) {
        currentOutputReg := 0.U
        spiPort.dataOut := 0.U
      }
    }
    
    is(State.instructionTransmit) {
      spiPort.chipSelect := false.B

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          when(instruction === SPIInstructions.readJEDECInstruction){  // ReadJeDECInstruction
            stateReg := State.receiveData
            pointerReg := 23.U
          }.elsewhen(instruction === SPIInstructions.writeEnableInstruction || 
                    instruction === SPIInstructions.chipEraseInstruction){ // WriteEnableInstruction or ChipEraseInstruction
            stateReg := State.finished
          }.elsewhen(instruction === SPIInstructions.readStatusRegister1Instruction) {
            stateReg := State.receiveData
            pointerReg := 7.U
          }.otherwise{ 
            stateReg := State.sendAddress
            pointerReg := 23.U 
          }
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }

      when (fallingEdgeOfSPIClk) {
        currentOutputReg := instruction(pointerReg)
        spiPort.dataOut := instruction(pointerReg)
      }
    }
    
    is (State.sendAddress) {
      spiPort.chipSelect := false.B

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          when(instruction === SPIInstructions.pageProgramInstruction){
            stateReg := State.writeData
            pointerReg := 31.U
          }.elsewhen(instruction === SPIInstructions.readDataInstruction){
            stateReg := State.receiveData
            pointerReg := 31.U
          }.otherwise {
            stateReg := State.receiveData
            pointerReg := 7.U
          }
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }

      when (fallingEdgeOfSPIClk) {
        currentOutputReg := address(pointerReg)
        spiPort.dataOut := address(pointerReg)
      }
    }

    is (State.writeData)
    {
      spiPort.chipSelect := false.B

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          stateReg := State.finished
          currentOutputReg := 0.U
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }

      when (fallingEdgeOfSPIClk) {
        currentOutputReg := data(pointerReg)
        spiPort.dataOut := data(pointerReg)
      }
    }

    // -- obtaining the data from the device --
    is (State.receiveData)
    {
      spiPort.chipSelect := false.B

      when(risingEdgeOfSPIClk) {
        dataOutReg(pointerReg) := spiPort.dataIn
        when(pointerReg === 0.U) {
          stateReg := State.finished
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }

      when (fallingEdgeOfSPIClk) {
        currentOutputReg := 0.U
        spiPort.dataOut := 0.U
      }
    }

    is (State.finished)
    {
      spiPort.spiClk := false.B
      spiClkReg := false.B

      when(interconnectPort.ready) {
        dataOutReg := VecInit(Seq.fill(32)(0.U(1.W))) // reset the dataOut register
        stateReg := State.idle
      }

      when (fallingEdgeOfSPIClk) {
        currentOutputReg := 0.U
        spiPort.dataOut := 0.U
      }

      interconnectPort.done := true.B
    }
  }
}
