import chisel3._
import chisel3.util._

class qspiIO extends Bundle {
  val spiClk = Output(Bool())
  val chipSelect = Output(Bool())

  val data0In = Input(Bool())
  val data1In = Input(Bool())
  val data2In = Input(Bool())
  val data3In = Input(Bool())
  val data0Out = Output(Bool())
  val data1Out = Output(Bool())
  val data2Out = Output(Bool())
  val data3Out = Output(Bool())
}

// Currently has a flash read implementation (that is untested :) )

class SPIOffChipMemoryController(
    val addrWidth: Int = 24,
    val dataWidth: Int = 32,
    val spiFreq: Int = 1000000,
    val freq: Int = 50000000,
    val configuredIntoQSPI: Boolean = false
) extends Module {
  val interconnectPort = IO(new PipeCon(addrWidth))
  val qspiPort = IO(new qspiIO)

  object State extends ChiselEnum {
    val idle, configure_into_qspi_instr_transmit,
        configure_into_qspi_data_transmit, read_instr_transmit,
        write_instr_transmit, addr_transmit, read_data_transmit,
        write_data_transmit, waiting = Value
  }
  val configuredReg = RegInit(configuredIntoQSPI.B)

  val stateReg = RegInit(State.idle)

  val pointerReg = RegInit(0.U(32.W))
  val spiClkCounterReg = RegInit(0.U(32.W))

  val spiClkCounterMax = ((freq / spiFreq / 2) - 1).U

  val spiClkReg = RegInit(false.B)

  val risingEdgeOfSpiClk = !RegNext(spiClkReg) && spiClkReg

  val readInstruction = "b01101011".U // 0x6B (Read Data Bytes), table 8.1.3
  val writeInstruction = "b00110010".U // 0x32 (Page Program), table 8.1.3
  val writeStatusRegister2Instruction =
    "b00110001".U // 0x31 (Write Status Register 2), table 8.1.2
  val configureIntoQSPIInstruction =
    "b00000010".U // 0x03 (Write Status Register 1), tfigure 4.b

  val regAddr = RegInit(0.U(addrWidth.W))
  val dataOutRegs = RegInit(VecInit(Seq.fill(dataWidth)(0.U(1.W))))
  val dataInRegs = RegInit(VecInit(Seq.fill(dataWidth)(0.U(1.W))))

  qspiPort.data0Out := 0.U
  qspiPort.data1Out := 0.U
  qspiPort.data2Out := 0.U
  qspiPort.data3Out := 0.U
  qspiPort.chipSelect := false.B

  interconnectPort.ack := false.B

  when(spiClkCounterReg === spiClkCounterMax) {
    spiClkCounterReg := 0.U
    spiClkReg := !spiClkReg
  }.otherwise {
    spiClkCounterReg := spiClkCounterReg + 1.U
  }

  switch(stateReg) {
    is(State.idle) {
      qspiPort.chipSelect := true.B
      when(!configuredReg) {
        stateReg := State.configure_into_qspi_instr_transmit
        qspiPort.chipSelect := false.B
        pointerReg := 7.U
      }.elsewhen(interconnectPort.rd) { // we assume the address is valid
        stateReg := State.read_instr_transmit
        qspiPort.chipSelect := false.B
        pointerReg := 7.U
      }.elsewhen(interconnectPort.wrMask.contains(true.B)) {
        stateReg := State.write_instr_transmit
        dataInRegs := interconnectPort.wrData.asTypeOf(Vec(dataWidth, Bool()))
        qspiPort.chipSelect := false.B
        pointerReg := 7.U
      }
      regAddr := interconnectPort.address
    }
    is(State.configure_into_qspi_instr_transmit) {
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          stateReg := State.configure_into_qspi_data_transmit
          pointerReg := 7.U
        }
      }
      qspiPort.data0Out := writeStatusRegister2Instruction(pointerReg)
    }
    is(State.configure_into_qspi_data_transmit) {
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          configuredReg := true.B
          stateReg := State.idle
          pointerReg := 0.U
        }
      }

      qspiPort.data0Out := configureIntoQSPIInstruction(pointerReg)
    }
    is(State.read_instr_transmit) {
      // after 8 clock cycles, the instruction is transmitted
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          stateReg := State.addr_transmit
          pointerReg := 23.U
        }
      }

      qspiPort.data0Out := readInstruction(pointerReg)
    }
    is(State.addr_transmit) {

      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        // same reason as before, only change after the last values have been read
        when(pointerReg === 0.U) {
          stateReg := State.waiting
          pointerReg := 8.U
        }
      }

      qspiPort.data0Out := regAddr(pointerReg)

    }
    is(State.waiting) {
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          stateReg := State.read_data_transmit
          pointerReg := 32.U
        }
      }

    }
    is(State.read_data_transmit) {
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 4.U

        when(pointerReg === 0.U) {
          stateReg := State.idle
          pointerReg := 0.U
          interconnectPort.ack := true.B
        }
      }

      dataOutRegs(
        pointerReg - 1.U
      ) := qspiPort.data3In // 31, 27, 23, 19, 15, 11, 7, 3
      dataOutRegs(
        pointerReg - 2.U
      ) := qspiPort.data2In // 30, 26, 22, 18, 14, 10, 6, 2
      dataOutRegs(
        pointerReg - 3.U
      ) := qspiPort.data1In // 29, 25, 21, 17, 13,  9, 5, 1
      dataOutRegs(
        pointerReg - 4.U
      ) := qspiPort.data0In // 28, 24, 20, 16, 12,  8, 4, 0
    }
    is(State.write_instr_transmit) {
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 1.U

        when(pointerReg === 0.U) {
          stateReg := State.addr_transmit
          pointerReg := 23.U
        }
      }

      qspiPort.data0Out := writeInstruction(pointerReg)
    }
    is(State.write_data_transmit) {
      when(risingEdgeOfSpiClk) {
        pointerReg := pointerReg - 4.U

        when(pointerReg === 0.U) {
          stateReg := State.idle
          pointerReg := 0.U
          interconnectPort.ack := true.B
        }
      }

      qspiPort.data0Out := dataInRegs(pointerReg - 1.U)
      qspiPort.data1Out := dataInRegs(pointerReg - 2.U)
      qspiPort.data2Out := dataInRegs(pointerReg - 3.U)
      qspiPort.data3Out := dataInRegs(pointerReg - 4.U)
    }
  }

  qspiPort.spiClk := spiClkReg
  // Concatenate the dataOutRegs to form the output data
  interconnectPort.rdData := dataOutRegs.reduce(_ ## _)
}
