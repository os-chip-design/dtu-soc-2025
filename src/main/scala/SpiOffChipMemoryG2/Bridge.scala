import chisel3._
import chisel3.util._

class Bridge(
    val spiFreq: Int = 1000000,
    val freq: Int = 50000000,
    val addrWidth: Int = 24,
) extends Module {
  
  val spiPort = IO(new spiIO)
  val pipeCon = IO(new PipeCon(addrWidth))
  val debug   = IO(new Bundle {
    val jedec = Input(Bool())
  })

  object State extends ChiselEnum {
    val idle, jedec, write0, write1, read0 = Value        
  }

  // Registers
  val stateReg = RegInit(State.idle)

  def mask(data: UInt, mask: UInt): UInt = {
    val maskedData = VecInit(Seq.fill(32)(0.U(1.W)))
    var maskIndex = 0
    for (i <- 0 until 32) {
      maskedData(i) := Mux(mask(maskIndex), data(i), 0.U(1.W))
      if (i % 8 == 7) { // 8 bits = 1 byte
        maskIndex += 1
      }
    }
    maskedData.reduce(_ ## _)
  }

  val spiController = Module(new SPIController(spiFreq, freq, addrWidth, 32))
  spiPort <> spiController.spiPort
  spiController.interconnectPort.address := pipeCon.address
  spiController.interconnectPort.dataIn  := mask(pipeCon.wrData, pipeCon.wrMask)
  pipeCon.rdData := spiController.interconnectPort.dataOut

  spiController.interconnectPort.valid := false.B
  spiController.interconnectPort.ready := false.B
  spiController.interconnectPort.currInstr := 0.U // Read JEDEC ID Instruction


  pipeCon.ack := false.B

  // TODO: how should we support read / write at the same time. 

  switch(stateReg) {
    is(State.idle) {
      when (debug.jedec) {
        stateReg := State.jedec
      }.elsewhen (pipeCon.rd) {
        stateReg := State.read0
      }.elsewhen (pipeCon.wr) {
        stateReg := State.write0
      }
    }

    is (State.jedec) {
      spiController.interconnectPort.currInstr := 0.U // Read JEDEC ID Instruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done) {
        stateReg := State.idle
        pipeCon.ack := true.B
      }
    }

    is (State.read0) {
      spiController.interconnectPort.currInstr := 3.U // Read Data Instruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done) {
        stateReg := State.idle
        pipeCon.ack := true.B
      }
    } 

    is (State.write0) {
      spiController.interconnectPort.currInstr := 1.U // Write Enable Instruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done) {
        stateReg := State.write1
      }
    }

    is (State.write1) {
      spiController.interconnectPort.currInstr := 2.U // Page Program Instruction
      spiController.interconnectPort.valid := true.B
      spiController.interconnectPort.ready := true.B

      when (spiController.interconnectPort.done) {
        stateReg := State.idle
        pipeCon.ack := true.B
      }
    }
  }
}
