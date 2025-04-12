import chisel3._
import chisel3.util._


class SPIWriteRead(
    val spiFreq: Int = 1000000,
    val freq: Int = 50000000,
    val addrWidth: Int = 24,
    val dataWidth: Int = 32,
    val refreshRate: Int = 1000000, // for showing the data on the seven segment display
) extends Module {
  val spiPort = IO(new spiIO)
  val interconnectPort = IO(new Bundle { // temporary interconnect port for testing
    val address = Input(UInt(8.W))
    val rd = Input(Bool()) // currently used as control signals for testing
    val wr = Input(Bool()) // currently used as control signals for testing
    //val rdData = Output(UInt(dataWidth.W))
    //val wrData = Input(UInt(dataWidth.W))
    val wrMask = Input(UInt(4.W))
    val ack = Output(Bool())
  })
  
  val io = IO(new Bundle { // connections for FPGA testing
    val currInstr = Input(UInt(3.W))
    val display_select = Input(UInt(3.W))
    val seg = Output(UInt(7.W))
    val an = Output(UInt(4.W))
    val state = Output(UInt(3.W))
  })
  
  object State extends ChiselEnum {
    val start, spiInstrTransmit,sendAddress,
        receiveData, writeData, finished= Value        
  }

  def risingEdge(x: Bool) = x && !RegNext(x)

  // Register 
  val stateReg = RegInit(State.start)
  val dataOutReg = RegInit(VecInit(Seq.fill(dataWidth)(0.U(1.W))))
  val pointerReg = RegInit(0.U(32.W))
  //  SPI clock counter /////////
  val spiClkCounterReg = RegInit(0.U(32.W))
  val spiClkCounterMax = ((freq / spiFreq / 2) - 1).U 
  // e.g. 50MHz / 1MHz = 50, 50/2 - 1 = 24, so every 25th clock cycle we toggle the SPI clock, causing a 1MHz clock
  val spiClkReg = RegInit(false.B)
  val risingEdgeOfSPIClk = risingEdge(spiClkReg)

  //-------------------------------
  val pageProgramInstruction =  "b00000010".U
  val writeEnableInstruction =  "b00000110".U // 0x06 (Write Enable), table 8.13
  val readJEDECInstruction   =  "b10011111".U
  val readDataInstruction    =  "b00000011".U // 0x03 (Read Data), table 8.1.3
  val readStatusRegisterInstruction = "b00000101".U // 0x05 (Read Status Register), table 8.1.3
  val sectorEraseInstruction = "b00100000".U // 0x20 (Sector Erase), table 8.1.3

  val addressReg = RegInit(0.U(addrWidth.W)) // Stores the address to access in the flash memory
  //val data = "b01000100010001001111111111111111".U(32.W) // Data to be written to the flash memory
  val data = "hDEADBEEF".U(32.W) // Data to be written to the flash memory
  val instrReg = RegInit(0.U(8.W))

  val rdData = WireDefault(0.U(dataWidth.W))

  spiPort.dataOut         := 0.U
  spiPort.spiClk          := spiClkReg
  spiPort.chipSelect      := true.B
  rdData := dataOutReg.reverse.reduce(_ ## _)
  interconnectPort.ack    := false.B

  when(spiClkCounterReg === spiClkCounterMax) {
    spiClkCounterReg := 0.U
    spiClkReg := !spiClkReg
  }.otherwise {
    spiClkCounterReg := spiClkCounterReg + 1.U
  }
  io.state := stateReg.asUInt
  switch(stateReg) {

    // --- starting ---
    is(State.start) {
      when (interconnectPort.rd) {
        spiPort.chipSelect := false.B
        stateReg := State.spiInstrTransmit
        pointerReg := 7.U

        instrReg := readJEDECInstruction // default instruction
        switch(io.currInstr) {
          is(1.U) { instrReg := writeEnableInstruction }
          is(2.U) { instrReg := pageProgramInstruction }
          is(3.U) { instrReg := readDataInstruction }
          is(4.U) { instrReg := readStatusRegisterInstruction }
          is(5.U) { instrReg := sectorEraseInstruction }
        }

        addressReg := addressReg(23, 8) ## interconnectPort.address(7, 0)
      }
    }
    

    // -- sending the command --
    is(State.spiInstrTransmit) {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          when(instrReg === readJEDECInstruction){  // ReadJeDECInstruction
            stateReg := State.receiveData
            pointerReg := 23.U
          }.elsewhen(instrReg === writeEnableInstruction){ // WriteEnableInstruction
            stateReg := State.finished
          }.otherwise{ 
            stateReg := State.sendAddress
            pointerReg := (addrWidth - 1).U 
          }
        }.otherwise {
          pointerReg := pointerReg - 1.U
        }
      }
      spiPort.dataOut := instrReg(pointerReg)
    }
    is (State.sendAddress) {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSPIClk) {
        when(pointerReg === 0.U) {
          when(instrReg === pageProgramInstruction){
            stateReg := State.writeData
            pointerReg := (dataWidth - 1).U
          }.elsewhen(instrReg === sectorEraseInstruction){
            stateReg := State.finished
          }.elsewhen(instrReg === readDataInstruction){
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
      spiPort.dataOut := addressReg(pointerReg)
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
      spiPort.chipSelect := true.B
      interconnectPort.ack := true.B
      when(interconnectPort.wr)
      {
        stateReg := State.start
        dataOutReg := VecInit(Seq.fill(dataWidth)(0.U(1.W)))
        instrReg := 0.U
      }
    }
    

  }
  
  val displayDriver = Module(new DisplayDriver(refreshRate))
  displayDriver.io.input := "b1010101110111010".U(16.W) // ABBA, value to know when not displaying anything
  switch(io.display_select) {
    is(0.U) { // 000
      displayDriver.io.input := data(15, 0)
    }
    is(1.U) { // 001
      displayDriver.io.input := data(31, 16)
    }
    is(2.U) { // 010
      displayDriver.io.input := rdData(15, 0)
    }
    is(3.U) { // 011
      displayDriver.io.input := rdData(31, 16)
    } 
    is(4.U) { // 100 
      displayDriver.io.input := pointerReg(15, 0)
    }
    is(5.U) { // 101
      displayDriver.io.input := pointerReg(31, 16)
    }
  }

  io.seg := displayDriver.io.seg
  io.an := displayDriver.io.an 
}

object SPIWriteReadBasys3 extends App {
  val basys3ClockFreq = 100000000 // 100MHz
  val spiFreq =         1000000 // 1MHz
  val refreshRate =     100000  // 1ms refresh rate for the seven segment display
  (new chisel3.stage.ChiselStage).emitVerilog(new SPIWriteRead(spiFreq, basys3ClockFreq, 24, 32, refreshRate), Array("--target-dir", "generated"))
  //(new chisel3.stage.ChiselStage).emitVerilog(new DisplayDriver(refreshRate), Array("--target-dir", "generated"))
}