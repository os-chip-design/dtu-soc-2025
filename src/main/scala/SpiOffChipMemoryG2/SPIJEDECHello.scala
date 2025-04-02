import chisel3._
import chisel3.util._

class spiIO extends Bundle {
  val spiClk = Output(Bool())
  val chipSelect = Output(Bool())

  val dataIn = Input(Bool())
  val dataOut = Output(Bool())

}

class JEDECout extends Bundle{
  val dataOut = Output(UInt(24.W))
}

class SPIJEDECHello(
    val addrWidth: Int = 24,
    val dataWidth: Int = 32,
    val spiFreq: Int = 1000000,
    val freq: Int = 50000000,
    val configuredIntoQSPI: Boolean = false
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
  spiPort.chipSelect := false.B
  spiPort.spiClk := spiClkReg
  JEDECout.dataOut := JEDECReg.reduce(_ ## _)

  when(spiClkCounterReg === spiClkCounterMax) {
    spiClkCounterReg := 0.U
    spiClkReg := !spiClkReg
  }.otherwise {
    spiClkCounterReg := spiClkCounterReg + 1.U
  }


  switch(stateReg) {
    // --- starting ---
    is(State.start) {
      spiPort.chipSelect := false.B
      pointerReg := 7.U
      stateReg := State.configure_into_spi_instr_transmit
    }

    // -- sending the command --
    is(State.configure_into_spi_instr_transmit) {
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
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          stateReg := State.finished
          pointerReg := 0.U
        }
        JEDECReg(pointerReg) := spiPort.dataIn
      }
    }

    is (State.finished)
    {
      // do nothing?
    }
  }
  
}