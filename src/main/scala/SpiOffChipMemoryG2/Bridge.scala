import chisel3._
import chisel3.util._

class Bridge() extends Module {
  
  val spiPort = IO(new spiIO)
  val pipeCon = IO(new PipeCon(24))
  val config = IO(new configIO)

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
  spiController.interconnectPort.mode := config.mode 
  pipeCon.rdData := spiController.interconnectPort.dataOut

  val targettingFlash = config.targetFlash

  spiController.interconnectPort.start := false.B
  spiController.interconnectPort.instruction := 0.U 

  val busy = spiController.interconnectPort.dataOut(31) // Busy flag from the flash memory (only valid following a read status register 1 instruction)
  val done = spiController.interconnectPort.done // Done flag from the spi controller

  val maskedData = mask(pipeCon.wrData, pipeCon.wrMask)

  pipeCon.ack := false.B

  switch(stateReg) {
    is(State.idle) {
      when (pipeCon.rd) {
        when (config.jedec && targettingFlash) {
          stateReg := State.jedec
        }.otherwise {
          stateReg := State.read0
          addressReg := pipeCon.address 
        }
      }.elsewhen (pipeCon.wr) {
        when (config.clear && targettingFlash) {
          stateReg := State.clear0
        }.otherwise {
            when (targettingFlash) {
              stateReg := State.write0
            }.otherwise {
              stateReg := State.write1
            }
            addressReg := pipeCon.address
            dataReg := maskedData
        }
      }
    }

    is (State.jedec) {
      spiController.interconnectPort.instruction := Instructions.readJEDECInstruction
      spiController.interconnectPort.start := true.B

      when (done) {
        stateReg := State.idle
        pipeCon.ack := true.B
      }
    }

    is (State.read0) {
      spiController.interconnectPort.instruction := Instructions.readDataInstruction
      spiController.interconnectPort.start := true.B

      when (done) {
        stateReg := State.idle
        pipeCon.ack := true.B
      }
    } 

    is (State.write0) {
      spiController.interconnectPort.instruction := Instructions.writeEnableInstruction
      spiController.interconnectPort.start := true.B

      when (done) {
        stateReg := State.write1
      }
    }

    is (State.write1) {
      spiController.interconnectPort.instruction := Instructions.pageProgramInstruction
      spiController.interconnectPort.start := true.B


      when (done) {
        when (targettingFlash) {
          stateReg := State.write2
        }.otherwise {
          stateReg := State.idle
          pipeCon.ack := true.B
        }
      }
    }

    is (State.write2) {
      spiController.interconnectPort.instruction := Instructions.readStatusRegister1Instruction
      spiController.interconnectPort.start := true.B

      when (done && !busy) { 
        stateReg := State.idle
        pipeCon.ack := true.B
      }
    }

    is (State.clear0) {
      spiController.interconnectPort.instruction := Instructions.writeEnableInstruction
      spiController.interconnectPort.start := true.B


      when (done) {
        stateReg := State.clear1
      }
    }

    is (State.clear1) {
      spiController.interconnectPort.instruction := Instructions.chipEraseInstruction
      spiController.interconnectPort.start := true.B


      when (done) {
        stateReg := State.clear2
      }
    }

    is (State.clear2) {
      spiController.interconnectPort.instruction := Instructions.readStatusRegister1Instruction
      spiController.interconnectPort.start := true.B


      when (done && !busy) { 
        stateReg := State.idle
        pipeCon.ack := true.B
      }
    }
  }
}
