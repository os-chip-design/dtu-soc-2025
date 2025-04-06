import chisel3._
import chisel3.util._

class spiIO extends Bundle {
  val spiClk = Output(Bool())
  val chipSelect = Output(Bool())

  val dataIn = Input(Bool())
  val dataOut = Output(Bool())

}

class JEDECout extends Bundle{
  val start = Input(Bool())
  val dataOut = Output(UInt(24.W))
  val states = Output(UInt(2.W))
}

class SPIJEDECHello(
    val spiFreq: Int = 1000000,
    val freq: Int = 50000000
) extends Module {
  val spiPort = IO(new spiIO)
  val JEDECout = IO(new JEDECout)
  
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

  spiPort.dataOut := 0.U
  spiPort.spiClk := spiClkReg
  spiPort.chipSelect := true.B
  JEDECout.dataOut := JEDECReg.reduce(_ ## _)

  when(spiClkCounterReg === spiClkCounterMax) {
    spiClkCounterReg := 0.U
    spiClkReg := !spiClkReg
  }.otherwise {
    spiClkCounterReg := spiClkCounterReg + 1.U
  }
  JEDECout.states := 0.U


  switch(stateReg) {
    // --- starting ---
    is(State.start) {
      when (JEDECout.start) {
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
      JEDECout.states := 1.U
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
      JEDECout.states := 2.U
    }

    is (State.finished)
    {
      JEDECout.states := 3.U
    }
  }
  
}

object SPIJEDECBasys3 extends App {
  val basys3ClockFreq = 100000000 // 100MHz
  val spiFreq = 1000000 // 1MHz
  // Use ChiselStage instead of Driver
  (new chisel3.stage.ChiselStage).emitVerilog(new SPIJEDECHello(spiFreq, basys3ClockFreq), Array("--target-dir", "generated"))
}