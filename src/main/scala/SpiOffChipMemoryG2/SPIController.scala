import chisel3._
import chisel3.util._




class SPIController(
) extends Module {
  val spiPort = IO(new spiIO)

  val interconnectPort = IO(new Bundle {
    val start = Input(Bool())
    val instruction = Input(UInt(8.W))
    val dataIn = Input(UInt(32.W))
    val address = Input(UInt(24.W))
    val dataOut = Output(UInt(32.W))
    val done = Output(Bool())
    val clockDivision = Input(UInt(32.W))
    val mode = Input(Bool()) // SPI clock mode, 0 or 3
  })

  object State extends ChiselEnum {
    val idle, instructionTransmit, sendAddress,
        receiveData, writeData, finished, waiting, syncFallEdgeFinish,
        preAddress, preWriteData, preReceiveData = Value
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

  // SPI clock mode, 0 (indicated by 0) or 3 (indicated by 1
  // always zero if not targeting the flash memory
  val idleClockMode = interconnectPort.mode

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
      when (interconnectPort.start) {
        spiClkReg := false.B
        spiClkCounterReg := 0.U
        stateReg := State.instructionTransmit
        pointerReg := 7.U
        currentOutputReg := instruction(7.U)
      }
      spiPort.spiClk := idleClockMode


      when (fallingEdgeOfSPIClk) {
        currentOutputReg := 0.U
        spiPort.dataOut := 0.U
      }
    }
    
    is(State.instructionTransmit) {
      spiPort.chipSelect := false.B

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          when(instruction === Instructions.readJEDECInstruction){  // ReadJeDECInstruction
            stateReg := State.preReceiveData
            pointerReg := 23.U
          }.elsewhen(instruction === Instructions.writeEnableInstruction || 
                    instruction === Instructions.chipEraseInstruction){ // WriteEnableInstruction or ChipEraseInstruction
            stateReg := State.syncFallEdgeFinish
          }.elsewhen(instruction === Instructions.readStatusRegister1Instruction) {
            stateReg := State.preReceiveData
            pointerReg := 7.U
          }.otherwise{ 
            stateReg := State.preAddress
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

    is (State.preAddress) {
      spiPort.chipSelect := false.B
      when (fallingEdgeOfSPIClk) {
        stateReg := State.sendAddress
        currentOutputReg := address(23.U)
        spiPort.dataOut := address(23.U)
      }
    }
    
    is (State.sendAddress) {
      spiPort.chipSelect := false.B

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          when(instruction === Instructions.pageProgramInstruction){
            stateReg := State.preWriteData
            pointerReg := 31.U
          }.elsewhen(instruction === Instructions.readDataInstruction){
            stateReg := State.preReceiveData
            pointerReg := 31.U
          }.elsewhen(instruction === Instructions.readStatusRegister1Instruction) {
            stateReg := State.preReceiveData
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

    is (State.preWriteData) {
      spiPort.chipSelect := false.B
      when (fallingEdgeOfSPIClk) {
        stateReg := State.writeData
        currentOutputReg := data(pointerReg)
        spiPort.dataOut := data(pointerReg)
      }
    }

    is (State.writeData)
    {
      spiPort.chipSelect := false.B

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          stateReg := State.syncFallEdgeFinish
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }

      when (fallingEdgeOfSPIClk) {
        currentOutputReg := data(pointerReg)
        spiPort.dataOut := data(pointerReg)
      }
    }

    is (State.preReceiveData) {
      spiPort.chipSelect := false.B
      when (fallingEdgeOfSPIClk) {
        pointerReg := pointerReg + 1.U
        stateReg := State.receiveData
        currentOutputReg := 0.U
        spiPort.dataOut := 0.U
      }
    }

    is (State.receiveData)
    {
      spiPort.chipSelect := false.B

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          stateReg := State.finished
          spiPort.spiClk := idleClockMode
          spiPort.chipSelect := true.B
        }.otherwise {
          pointerReg := pointerReg - 1.U
          dataOutReg(pointerReg - 1.U) := spiPort.dataIn
        }
      }
    }

    is (State.syncFallEdgeFinish
    ) {
      spiPort.chipSelect := false.B
      when (risingEdgeOfSPIClk){
        stateReg := State.finished
        spiPort.spiClk := idleClockMode
        spiPort.chipSelect := true.B
      }

      when (fallingEdgeOfSPIClk) {
        currentOutputReg := 0.U
        spiPort.dataOut := 0.U
      }
    }

    is (State.finished)
    {
      spiPort.spiClk := idleClockMode

      dataOutReg := VecInit(Seq.fill(32)(0.U(1.W))) // reset the dataOut register
      stateReg := State.waiting
      pointerReg := 1.U

      interconnectPort.done := true.B
    }

    is (State.waiting) {
      spiPort.chipSelect := true.B
      spiPort.spiClk := idleClockMode

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          stateReg := State.idle
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }
    }
  }
}
