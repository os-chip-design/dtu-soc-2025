import chisel3._
import chisel3.util._

class Bridge(
    val clockDivision : Int = 50,
) extends Module {
  
  val spiPort = IO(new spiIO)
  val pipeCon = IO(new PipeCon(24))
  val debug   = IO(new Bundle {
    val jedec = Input(Bool())
    val clear = Input(Bool())
  })
  val config = IO(new Bundle {
    val flashMemory = Input(Bool()) // toggle to indicate targetting flash memory / RAM
  })

  object State extends ChiselEnum {
    val idle, jedec, write0, write1, write2, read0, clear0, clear1 = Value        
  }

  // Registers
  val stateReg = RegInit(State.idle)

  def mask(data: UInt, mask: UInt): UInt = {
    val maskedData = VecInit(Seq.fill(32)(0.U(1.W)))
    var maskIndex = 0
    for (i <- 0 until 32) {
      maskedData(i) := Mux(mask(maskIndex), data(i), 0.U(1.W))
      if (i % 8 == 7) { // 7, 15, 23, 31
        maskIndex += 1
      }
    }
    maskedData.reduce(_ ## _)
  }

  val spiController = Module(new SPIController(clockDivision))
  spiPort <> spiController.spiPort
  spiController.interconnectPort.address := pipeCon.address
  spiController.interconnectPort.dataIn  := mask(pipeCon.wrData, pipeCon.wrMask)
  pipeCon.rdData := spiController.interconnectPort.dataOut

  spiController.interconnectPort.valid := false.B
  spiController.interconnectPort.ready := false.B
  spiController.interconnectPort.instruction := 0.U // Read JEDEC ID Instruction
  spiController.interconnectPort.flashMemory := config.flashMemory

  pipeCon.ack := false.B

  // TODO: how should we support read / write at the same time. 
  // TODO: check the pipecon at https://github.com/t-crest/soc-comm to see if all rules are covered

  switch(stateReg) {
    is(State.idle) {
      when (debug.jedec) {
        stateReg := State.jedec
      }.elsewhen (pipeCon.rd) {
        stateReg := State.read0
      }.elsewhen (pipeCon.wr && config.flashMemory) {
        stateReg := State.write0
      }.elsewhen (pipeCon.wr) {
        stateReg := State.write1
      }.elsewhen (debug.clear) {
        stateReg := State.clear0
      }
    }

    is (State.jedec) {
      spiController.interconnectPort.instruction := SPIInstructions.readJEDECInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done) {
        stateReg := State.idle
        pipeCon.ack := true.B
      }
    }

    is (State.read0) {
      spiController.interconnectPort.instruction := SPIInstructions.readDataInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done) {
        stateReg := State.idle
        pipeCon.ack := true.B
      }
    } 

    is (State.write0) {
      spiController.interconnectPort.instruction := SPIInstructions.writeEnableInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done) {
        stateReg := State.write1
      }
    }

    is (State.write1) {
      spiController.interconnectPort.instruction := SPIInstructions.pageProgramInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done) {
        stateReg := State.write2
      }
    }

    is (State.write2) {
      spiController.interconnectPort.instruction := SPIInstructions.readStatusRegister1Instruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done && !spiController.interconnectPort.dataOut(0)) { 
        pipeCon.ack := true.B
        stateReg := State.idle
      }
    }

    is (State.clear0) {
      spiController.interconnectPort.instruction := SPIInstructions.chipEraseInstruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done) {
        stateReg := State.clear1
      }
    }

    is (State.clear1) {
      spiController.interconnectPort.instruction := SPIInstructions.readStatusRegister1Instruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done && !spiController.interconnectPort.dataOut(0)) { 
        pipeCon.ack := true.B
        stateReg := State.idle
      }
    }
  }
}
