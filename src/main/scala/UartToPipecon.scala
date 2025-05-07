import chisel3._
import chisel3.util._

class UartToPipecon(
                     addrWidth: Int,
                     clockSpeed: Int,
                     baudRate: Int
                   ) extends Module {
  val io = IO(new PipeCon(addrWidth))
  val out = IO(new Bundle {
    val rxOut = Input(Bool())
    val ctsOut = Input(Bool())
    val txOut = Output(Bool())
    val rtsOut = Output(Bool())
  })



  // Instantiate the UART module
  val uartMod = Module(new UartModule(clockSpeed, baudRate))

  // Define register addresses (offset from base address)
  val TX_DATA_ADDR = 0x0
  val RX_DATA_ADDR = 0x4
  val STATUS_ADDR = 0x8
  val CONTROL_ADDR = 0xC

  // Status register bit definitions, 0: tx_Ready, 1: rx_valid
  val statusReg = Wire(UInt(2.W))
  statusReg := Cat(uartMod.io.rx_valid, uartMod.io.tx_ready)

  // control register bit definitions:
  // only flow control
  val controlReg = RegInit(0.U(1.W))
  uartMod.io.flowControl := controlReg(0)

  // Default connections
  uartMod.io.tx_valid := false.B
  uartMod.io.tx_data := 0.U
  uartMod.io.rx_ready := true.B

  // default pipecone connections
  io.rdData := 0.U
  io.ack := false.B

  // connect UART physical pins
  uartMod.io.rx := out.rxOut  // exposed pin?
  uartMod.io.cts := out.ctsOut // exposed pin?
  out.txOut := uartMod.io.tx
  out.rtsOut := uartMod.io.rts

  // handshaking state machine
  val sIdle :: sReading :: sWriting :: sAck :: Nil = Enum(4)
  val state = RegInit(sIdle)

  //address decoder - determine which register is being accessed
  val addrDecoded = io.address(3, 2)  // bits 3:2 select the register


  // state machine for handling read and write operations
  switch(state) {
    is(sIdle) {
      when(io.rd) {
        state := sReading
      }.elsewhen(io.wr) {
        state := sWriting
      }.otherwise {
        state := sIdle
      }
    }

    is(sReading) {
      // handle read operations based on address
      switch(addrDecoded) {
        is(TX_DATA_ADDR.U(4.W)) {
          io.rdData := 0.U  // TX data cannot be read back
        }
        is(RX_DATA_ADDR.U(4.W)) {
          io.rdData := uartMod.io.rx_data & 0xFF.U  // read received data
        }
        is(STATUS_ADDR.U(4.W)) {
          io.rdData := statusReg.asUInt  // read status register
        }
        is(CONTROL_ADDR.U(4.W)) {
          io.rdData := controlReg.asUInt  // read control register
        }
      }
      state := sAck
    }

    is(sWriting) {
      // Handle write operations based on address
      switch(addrDecoded) {
        is(TX_DATA_ADDR.U(4.W)) {
          when(uartMod.io.tx_ready) {
            uartMod.io.tx_data := io.wrData(7, 0)  // Write data to transmit
            uartMod.io.tx_valid := true.B  // Signal valid data
            state := sAck
          }
          // wait until uart is ready
        }
        is(RX_DATA_ADDR.U(4.W)) {
          // read only
          state := sAck
        }
        is(STATUS_ADDR.U(4.W)) {
          // read only
          state := sAck
        }
        is(CONTROL_ADDR.U(4.W)) {
          // wwrite to control register - update based on write mask
          when(io.wrMask(0)) {
            controlReg := io.wrData(0)  // Only write to bit 0 (flowControl)
          }
          state := sAck
        }
      }
    }

    is(sAck) {
      io.ack := true.B  // assert ack

      // for tx operations, deassert tx_valid after one cycle
      //uartMod.io.tx_valid := false.B

      // Return to idle state
      state := sIdle
    }
  }
}