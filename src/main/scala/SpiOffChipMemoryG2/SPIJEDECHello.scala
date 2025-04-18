import chisel3._
import chisel3.util._

class SPIJEDECHello(
    val spiFreq: Int = 1000000,
    val freq: Int = 50000000,
    val addrWidth: Int = 24,
) extends Module {
  val spiPort = IO(new spiIO)
  val interconnectPort = IO(new PipeCon(addrWidth))
  
  object State extends ChiselEnum {
    val start, configure_into_spi_instr_transmit,
        receiveData, finished = Value        
  }

  val stateReg = RegInit(State.start)
  
  val JEDECReg = RegInit(VecInit(Seq.fill(24)(0.U(1.W))))

  val pointerReg = RegInit(0.U(32.W))
  
  val spiClkCounterReg = RegInit(0.U(32.W))

  val spiClkCounterMax = ((freq / spiFreq / 2) - 1).U

  val spiClkReg = RegInit(false.B)

  val risingEdgeOfSpiClk = !RegNext(spiClkReg) && spiClkReg
  val readJEDECInstruction = "b10011111".U
  val writeEnableInstruction = "b00000110".U // 0x06 (Write Enable), table 8.1.3



  spiPort.dataOut := 0.U
  spiPort.spiClk := spiClkReg
  spiPort.chipSelect := true.B
  interconnectPort.rdData := JEDECReg.reduce(_ ## _)
  interconnectPort.ack := false.B

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
        stateReg := State.configure_into_spi_instr_transmit
        pointerReg := 7.U
      }
    }

    // -- sending the command --
    is(State.configure_into_spi_instr_transmit) {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          stateReg := State.receiveData
          pointerReg := 23.U
        }
      }
      spiPort.dataOut := readJEDECInstruction(pointerReg)
    }

    // -- obtaining the data from the device --
    is (State.receiveData)
    {
      spiPort.chipSelect := false.B
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          stateReg := State.finished
        }
        JEDECReg(pointerReg) := spiPort.dataIn
      }
    }

    is (State.finished)
    {
      interconnectPort.ack := true.B
    }
  }
  
}

object SPIJEDECBasys3 extends App {
  val basys3ClockFreq = 100000000 // 100MHz
  val spiFreq = 1000000 // 1MHz
  // Use ChiselStage instead of Driver
  (new chisel3.stage.ChiselStage).emitVerilog(new SPIJEDECHello(spiFreq, basys3ClockFreq), Array("--target-dir", "generated"))
}