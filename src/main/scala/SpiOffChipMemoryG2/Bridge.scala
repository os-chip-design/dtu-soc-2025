import chisel3._
import chisel3.util._

class Bridge() extends Module {
  
  val spiPort = IO(new spiIO)
  val pipeCon = IO(new PipeCon(24))
  val debug   = IO(new Bundle {
    val jedec = Input(Bool())
    val clear = Input(Bool())
  })
  val config = IO(new Bundle {
    val clockDivision = Input(UInt(32.W))
  })

  object State extends ChiselEnum {
    val idle, jedec, write0, write1, write2, read0, clear0, clear1, clear2 = Value        
  }

  val stateReg = RegInit(State.idle)

  // Need these two registers as we can't be sure that the data won't change in the middle of the transaction
  val addressReg = RegInit(0.U(24.W))
  val dataReg = RegInit(0.U(32.W))

  // Hardware generation of the mask
  def mask(data: UInt, mask: UInt): UInt = {
    val maskedData = VecInit(Seq.fill(32)(0.U(1.W)))
    var maskIndex = 0
    for (i <- 0 until 32) {
      maskedData(i) := data(i) & mask(maskIndex)
      if (i % 8 == 7) { // 7, 15, 23, 31
        maskIndex += 1
      }
    }
    maskedData.reduce(_ ## _)
  }

  val spiController = Module(new SPIController())
  spiPort <> spiController.spiPort
  spiController.interconnectPort.address := addressReg
  spiController.interconnectPort.dataIn  := dataReg
  spiController.interconnectPort.clockDivision := config.clockDivision
  pipeCon.rdData := spiController.interconnectPort.dataOut

  spiController.interconnectPort.valid := false.B
  spiController.interconnectPort.ready := false.B
  spiController.interconnectPort.instruction := 0.U 

  val busy = spiController.interconnectPort.dataOut(0) // Busy flag from the flash memory
  val done = spiController.interconnectPort.done // Done flag from the spi controller

  val maskedData = mask(pipeCon.wrData, pipeCon.wrMask)

  pipeCon.ack := false.B

  switch(stateReg) {
    is(State.idle) {
      when (debug.jedec) {
        stateReg := State.jedec
      }.elsewhen (pipeCon.rd) {
        stateReg := State.read0
        addressReg := pipeCon.address 
      }.elsewhen (pipeCon.wr) {
        stateReg := State.write0
        addressReg := pipeCon.address
        dataReg := maskedData
      }.elsewhen (debug.clear) {
        stateReg := State.clear0
      }
    }

    is (State.jedec) {
      spiController.interconnectPort.instruction := SPIInstructions.readJEDECInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (done) {
        when (debug.jedec) {
          stateReg := State.jedec
        }.elsewhen (pipeCon.rd) {
          stateReg := State.read0
          addressReg := pipeCon.address 
        }.elsewhen (pipeCon.wr) {
          stateReg := State.write0
          addressReg := pipeCon.address
          dataReg := maskedData
        }.elsewhen (debug.clear) {
          stateReg := State.clear0
        }.otherwise {
          stateReg := State.idle
        }
        pipeCon.ack := true.B
      }
    }

    is (State.read0) {
      spiController.interconnectPort.instruction := SPIInstructions.readDataInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (done) {
        when (debug.jedec) {
          stateReg := State.jedec
        }.elsewhen (pipeCon.rd) {
          stateReg := State.read0
          addressReg := pipeCon.address 
        }.elsewhen (pipeCon.wr) {
          stateReg := State.write0
          addressReg := pipeCon.address
          dataReg := maskedData
        }.elsewhen (debug.clear) {
          stateReg := State.clear0
        }.otherwise {
          stateReg := State.idle
        }
        pipeCon.ack := true.B
      }
    } 

    is (State.write0) {
      spiController.interconnectPort.instruction := SPIInstructions.writeEnableInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (done) {
        stateReg := State.write1
      }
    }

    is (State.write1) {
      spiController.interconnectPort.instruction := SPIInstructions.pageProgramInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (done) {
        stateReg := State.write2
      }
    }

    is (State.write2) {
      spiController.interconnectPort.instruction := SPIInstructions.readStatusRegister1Instruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (done && !busy) { 
        when (debug.jedec) {
          stateReg := State.jedec
        }.elsewhen (pipeCon.rd) {
          stateReg := State.read0
          addressReg := pipeCon.address 
        }.elsewhen (pipeCon.wr) {
          stateReg := State.write0
          addressReg := pipeCon.address
          dataReg := maskedData
        }.elsewhen (debug.clear) {
          stateReg := State.clear0
        }.otherwise {
          stateReg := State.idle
        }
        pipeCon.ack := true.B
      }
    }

    is (State.clear0) {
      spiController.interconnectPort.instruction := SPIInstructions.writeEnableInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (done) {
        stateReg := State.clear1
      }
    }

    is (State.clear1) {
      spiController.interconnectPort.instruction := SPIInstructions.chipEraseInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (done) {
        stateReg := State.clear2
      }
    }

    is (State.clear2) {
      spiController.interconnectPort.instruction := SPIInstructions.readStatusRegister1Instruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (done && !busy) { 
        when (debug.jedec) {
          stateReg := State.jedec
        }.elsewhen (pipeCon.rd) {
          stateReg := State.read0
          addressReg := pipeCon.address 
        }.elsewhen (pipeCon.wr) {
          stateReg := State.write0
          addressReg := pipeCon.address
          dataReg := maskedData
        }.elsewhen (debug.clear) {
          stateReg := State.clear0
        }.otherwise {
          stateReg := State.idle
        }
        pipeCon.ack := true.B
      }
    }
  }
}
