import chisel3._
import chisel3.util._

class SpiMemTop extends Module {
  val io = IO(new Bundle {
    val spiMiso = Input(Bool())
    val spiMosi = Output(Bool())
    val spiCs = Output(Bool())
    val spiClk = Output(Bool())

    val seg = Output(UInt(7.W))
    val an = Output(UInt(4.W))

    val leds = Output(UInt(8.W))

    val CS0 = Output(Bool())
    val CS2 = Output(Bool())
  })

  object State extends ChiselEnum {
    val sIdle, sSendCmd, sWait, sDone = Value
  }

  import State._

  val state = RegInit(sIdle)

  val controller = Module(new SpiMem)

  // connect spi lines
  io.spiMosi := controller.io.spiMosi
  io.spiCs := controller.io.spiCs
  io.spiClk := controller.io.spiClk
  controller.io.spiMiso := io.spiMiso

  // display connection
  io.seg := controller.io.seg
  io.an := controller.io.an

  controller.io.cpuCmd := 0x9F.U
  controller.io.cpuWriteData := 0.U
  controller.io.sendLength := 8.U
  controller.io.numWaitCycles := 0.U
  controller.io.receiveLength := 24.U

  io.CS0 := false.B
  io.CS2 := false.B

  val delayCounter = RegInit(0.U(24.W))

  controller.io.enable := false.B

  switch(state) {
    is(sIdle) {
      delayCounter := delayCounter + 1.U
      when(delayCounter === 0xF.U) {
        state := sSendCmd
      }
    }
    is(sSendCmd) {
      controller.io.enable := true.B
      state := sWait
    }
    is(sWait) {
      when(controller.io.done) {
        state := sDone
      }
    }
    is(sDone) {

    }
  }

  // ----------------------
  // LED debug indicators:
  // ----------------------
  // LEDs: [7:0]
  // [7:6] = FSM state (2 bits)
  // [5]   = CS# status (active low)
  // [4]   = SPI Clock status
  // [3:0] = controller bitCounter lower 4 bits
  io.leds := Cat(
    state.asUInt, // [7:6] FSM state
    ~controller.io.spiCs, // [5] CS# active low (LED ON when CS is low)
    controller.io.spiClk, // [4] SPI clock (blinks)
    //controller.bitCounter(3, 0) // [3:0] Lower 4 bits of bitCounter
  )
}

object SpiMemTop extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SpiMemTop(), Array("--target-dir", "generated"))
}