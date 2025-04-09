import chisel3._
import chisel3.util._


class SPIWriteRead(
    val spiFreq: Int = 1000000,
    val freq: Int = 50000000,
    val addrWidth: Int = 24,
) extends Module {
  val spiPort = IO(new spiIO)
  val interconnectPort = IO(new PipeCon(addrWidth))
  
  val io = IO(new Bundle {
    val currInstr = Input(UInt(2.W))
  })
  
  object State extends ChiselEnum {
    val start, spiInstrTransmit,sendAddress,
        receiveData, writeData= Value        
  }
  // Register 
  val stateReg = RegInit(State.start)
  val dataOutReg = RegInit(VecInit(Seq.fill(16)(0.U(1.W))))
  val pointerReg = RegInit(0.U(32.W))
  //  SPI clock counter /////////
  val spiClkCounterReg = RegInit(0.U(32.W))
  val spiClkCounterMax = ((freq / spiFreq / 2) - 1).U
  val spiClkReg = RegInit(false.B)
  val risingEdgeOfSpiClk = !RegNext(spiClkReg) && spiClkReg

  //-------------------------------
  val pageProgramInstruction =  "b00000010".U
  val writeEnableInstruction =  "b00000110".U // 0x06 (Write Enable), table 8.13
  val readDataInstruction    =  "b00000011".U // 0x03 (Read Data), table 8.1.3
  val address = "b000000000000000000000000".U 

  spiPort.dataOut         := 0.U
  spiPort.spiClk          := spiClkReg
  spiPort.chipSelect      := true.B
  interconnectPort.rdData := dataOutReg.reduce(_ ## _)
  interconnectPort.ack    := false.B

  when(spiClkCounterReg === spiClkCounterMax) {
    spiClkCounterReg := 0.U
    spiClkReg := !spiClkReg
  }.otherwise {
    spiClkCounterReg := spiClkCounterReg + 1.U
  }

  switch(stateReg) {
    // --- starting ---
    is(State.start) {
      when (interconnectPort.rd) {
        spiPort.chipSelect := false.B
        stateReg := State.spiInstrTransmit
        pointerReg := 7.U
      }
    }

    // -- sending the command --
    is(State.spiInstrTransmit) {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          when(io.currInstr === 0.U){
            stateReg := State.start
            pointerReg := 0.U
            interconnectPort.ack := true.B        
          }.otherwise{
            stateReg := State.sendAddress
            pointerReg := 23.U
          }
        }
      }

      switch(io.currInstr) {
        is(0.U) {
          spiPort.dataOut := writeEnableInstruction(pointerReg)
        }
        
        is(1.U) {
          spiPort.dataOut := pageProgramInstruction(pointerReg)          
        }

        is(2.U){
          spiPort.dataOut := readDataInstruction(pointerReg)
        }
      }
    }

    is (State.sendAddress) {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          when(io.currInstr === 1.U){
            stateReg := State.writeData
            pointerReg := 15.U
          }.otherwise {
            stateReg := State.receiveData
            pointerReg := 15.U
          }
        }
      }
      spiPort.dataOut := address(pointerReg)
    }

    is (State.writeData)
    {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          stateReg := State.start
          interconnectPort.ack := true.B        
        }

        spiPort.dataOut := interconnectPort.wrData(pointerReg)
      }
    }

    // -- obtaining the data from the device --
    is (State.receiveData)
    {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          stateReg := State.start
          interconnectPort.ack := true.B
        }

        dataOutReg(pointerReg) := spiPort.dataIn
      }
    }
  }
  
}

object SPIWriteReadBasys3 extends App {
  val basys3ClockFreq = 100000000 // 100MHz
  val spiFreq = 1000000 // 1MHz
  // Use ChiselStage instead of Driver
  (new chisel3.stage.ChiselStage).emitVerilog(new SPIWriteRead(spiFreq, basys3ClockFreq), Array("--target-dir", "generated"))
}