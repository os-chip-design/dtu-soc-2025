import chisel3._
import chisel3.util._




class SPIController(
    val clockDivision : Int = 50,
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

    val flashMemory = Input(Bool()) // toggle to indicate targetting flash memory / RAM
  })

  object State extends ChiselEnum {
    val idle, instructionTransmit, sendAddress,
        receiveData, writeData, finished= Value        
  }

  def risingEdge(x: Bool) = x && !RegNext(x)

  // Registers
  val stateReg = RegInit(State.idle)
  val dataOutReg = RegInit(VecInit(Seq.fill(32)(0.U(1.W))))
  val pointerReg = RegInit(0.U(32.W))
  
  //  SPI clock counter 
  val spiClkCounterReg = RegInit(0.U(32.W))
  val spiClkCounterMax = ((clockDivision / 2) - 1).U 
  val spiClkReg = RegInit(false.B)
  val risingEdgeOfSPIClk = risingEdge(spiClkReg)

  val rdData = dataOutReg.reduce(_ ## _)

  val instruction = interconnectPort.instruction // Instruction to be sent to the flash memory

  val data = interconnectPort.dataIn // Data to be sent to the flash memory
  val address = interconnectPort.address // Address to be sent to the flash memory
  val startClock = WireDefault(false.B) // Start clock signal for the SPI communication
  
  spiPort.dataOut          := 0.U
  spiPort.chipSelect       := true.B
  spiPort.spiClk           := spiClkReg
  interconnectPort.done    := false.B
  interconnectPort.dataOut := rdData

  when (startClock) {
    when(spiClkCounterReg === spiClkCounterMax) {
      spiClkCounterReg := 0.U
      spiClkReg := !spiClkReg
    }.otherwise {
      spiClkCounterReg := spiClkCounterReg + 1.U
    }
  }.otherwise {
    spiClkCounterReg := 0.U
    spiClkReg := true.B
  }

  when (risingEdge(startClock)) {
    spiPort.spiClk := false.B
    spiClkReg := false.B
  }
  

  switch(stateReg) {

    is(State.idle) {
      when (interconnectPort.valid) {
        spiPort.chipSelect := false.B
        stateReg := State.instructionTransmit
        pointerReg := 7.U
        dataOutReg := VecInit(Seq.fill(32)(0.U(1.W))) // reset the dataOut register
        startClock := true.B
      }
    }
    
    is(State.instructionTransmit) {
      spiPort.chipSelect := false.B
      spiPort.dataOut := instruction(pointerReg)
      startClock := true.B 

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          when(instruction === SPIInstructions.readJEDECInstruction && interconnectPort.flashMemory){  // ReadJeDECInstruction
            stateReg := State.receiveData
            pointerReg := 23.U
          }.elsewhen(instruction === SPIInstructions.writeEnableInstruction || 
                    instruction === SPIInstructions.chipEraseInstruction){ // WriteEnableInstruction or ChipEraseInstruction
            stateReg := State.finished
            startClock := false.B
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
    }
    
    is (State.sendAddress) {
      spiPort.chipSelect := false.B
      spiPort.dataOut := address(pointerReg)
      startClock := true.B

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          when(instruction === SPIInstructions.pageProgramInstruction){
            stateReg := State.writeData
            pointerReg := 31.U
          }.elsewhen(instruction === SPIInstructions.readDataInstruction){
            stateReg := State.receiveData
            pointerReg := 31.U
          }.elsewhen(instruction === SPIInstructions.readJEDECInstruction && !interconnectPort.flashMemory){
            stateReg := State.receiveData
            pointerReg := 19.U
          }.otherwise {
            stateReg := State.receiveData
            pointerReg := 7.U
          }
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }
    }

    is (State.writeData)
    {
      spiPort.chipSelect := false.B
      spiPort.dataOut := data(pointerReg)
      startClock := true.B

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          stateReg := State.finished
          startClock := false.B
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }
    }

    // -- obtaining the data from the device --
    is (State.receiveData)
    {
      spiPort.chipSelect := false.B
      dataOutReg(pointerReg) := spiPort.dataIn
      startClock := true.B

      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          stateReg := State.finished
          startClock := false.B
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }
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
