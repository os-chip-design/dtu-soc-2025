import chisel3._
import chisel3.util._

///////////////////// Pin guide /////////////////////
// --- Transition inputs --- (fpga_start, fpga_again, fpga_jedec, fpga_justRead)
// From idle:
// xxx1 : read JEDEC ID -> goes to either done or error state
// xx10 : just read     -> goes to justRead1 state
// 1x00 : run normally  -> goes to either done or error state

// From done:
// x1xx : -> go to idle state

// From error:
// x1xx : -> go to idle state

// From justRead1:
// xx0x : -> go to done state
// x11x : -> go to justRead2 state

// From justRead2:
// 1xxx : -> go to justRead1 state

// Seven segment display (fpga_sel):
// 000 : readData[15:0] (SPI data)
// 001 : readData[31:16] (SPI data)
// 010 : data[pointerReg][15:0] (data to be written)
// 011 : data[pointerReg][31:16] (data to be written)
// 100 : address[pointerReg][15:0] (address to be written)
// 101 : address[pointerReg][23:16] (address to be written)
// 110 : pointerReg[15:0] (index)
// 111 : pointerReg[23:16] (index)
///////////////////// ////////// /////////////////////

class FPGATest(
    val spiFreq: Int = 1000000,
    val freq: Int = 50000000,
    val refreshRate: Int = 1000000, // for showing the data on the seven segment display
    val testCases: Int = 4,
) extends Module {
  
  val spiPort = IO(new spiIO)
  val fpga = IO(new Bundle {
    val start = Input(Bool())
    val again = Input(Bool()) 
    val jedec = Input(Bool())
    val justRead = Input(Bool())
    val error = Output(Bool())
    val done  = Output(Bool())

    val sel = Input(UInt(3.W))
    val seg = Output(UInt(7.W))
    val an  = Output(UInt(4.W))

    val states = Output(UInt(3.W))
  })

  object State extends ChiselEnum {
    val idle, jedec, reading, writing, justRead0, justRead1, justRead2, error, done = Value        
  }

  val expectedJEDEC = "h00EF7018".U // table 8.1.1

  // Registers
  val stateReg = RegInit(State.idle)

  fpga.states := stateReg.asUInt

  val bridge = Module(new Bridge(spiFreq, freq, 24))
  spiPort <> bridge.spiPort
  bridge.debug.jedec := false.B
  bridge.pipeCon.rd := false.B
  bridge.pipeCon.wr := false.B
  bridge.pipeCon.wrMask := "b1111".U
  
  val readData = bridge.pipeCon.rdData

  val pointerReg = RegInit(0.U(32.W))

  val dataOffset = 36
  val dataMultiplier = 4
  val data = VecInit(Seq.fill(testCases)(0.U(32.W))) 
  for (i <- 0 until testCases) {
    data(i) := (dataMultiplier*i + dataOffset).U(32.W)
    println(s"data($i) = ${dataMultiplier*i + dataOffset}")
  }

  val addressesOffset = 12
  val addressesMultiplier = 4
  val addresses = VecInit(Seq.fill(testCases)(0.U(24.W))) 
  for (i <- 0 until testCases) {
    addresses(i) := (addressesMultiplier*i + addressesOffset).U(24.W)
    println(s"addresses($i) = ${addressesMultiplier*i + addressesOffset}")
  }
  

  bridge.pipeCon.address := addresses(pointerReg)
  bridge.pipeCon.wrData  := data(pointerReg)

  val displayDriver = Module(new DisplayDriver(refreshRate))
  fpga.seg := displayDriver.io.seg
  fpga.an  := displayDriver.io.an
  displayDriver.io.input := "hABBA".U // default value for the display
  switch (fpga.sel) {
    is(0.U) { displayDriver.io.input := readData(15, 0) }                     // 000
    is(1.U) { displayDriver.io.input := readData(31, 16) }                    // 001
    is(2.U) { displayDriver.io.input := (data(pointerReg))(15, 0) }             // 010
    is(3.U) { displayDriver.io.input := (data(pointerReg))(31, 16) }            // 011
    is(4.U) { displayDriver.io.input := (addresses(pointerReg))(15, 0) }        // 100
    is(5.U) { displayDriver.io.input := (addresses(pointerReg))(23, 16) }       // 101
    is(6.U) { displayDriver.io.input := pointerReg(15, 0) }                   // 110
    is(7.U) { displayDriver.io.input := pointerReg(23, 16) }                  // 111
  }

  fpga.done := false.B
  fpga.error := false.B

  switch(stateReg) {
    is(State.idle) {
      when (fpga.jedec) {
        stateReg := State.jedec
      }.elsewhen (fpga.justRead) {
        stateReg := State.justRead0
      }.elsewhen (fpga.start) {
        stateReg := State.writing
      }
    }

    is(State.jedec) {
      bridge.debug.jedec := true.B
      when (bridge.pipeCon.ack) {
        when (bridge.pipeCon.rdData =/= expectedJEDEC) {
          stateReg := State.error
        }.otherwise {
          stateReg := State.done
        }
      }
    }

    is(State.writing) {
      bridge.pipeCon.wr := true.B
      when (bridge.pipeCon.ack) {
        stateReg := State.reading
      }
    }

    is(State.reading) {
      bridge.pipeCon.rd := true.B
      when (bridge.pipeCon.ack) {
        when (readData =/= data(pointerReg)) {
          stateReg := State.error
        }.otherwise {
          when (pointerReg === (testCases - 1).U) {
            stateReg := State.done
          }.otherwise {
            pointerReg := pointerReg + 1.U
            stateReg := State.writing
          }
        }
      }
    }

    is(State.justRead0) {
      bridge.pipeCon.rd := true.B
      when (bridge.pipeCon.ack) {
        stateReg := State.justRead1
      }
    }

    is(State.justRead1) {
      when (!fpga.justRead || (pointerReg === (testCases - 1).U)) {
        stateReg := State.done
      }.elsewhen (fpga.again) {
        stateReg := State.justRead2
        pointerReg := pointerReg + 1.U
      }
    }

    is(State.justRead2) {
      when (fpga.start) {
        stateReg := State.justRead0
      }
    }

    is(State.error) {
      fpga.error := true.B
      when (fpga.again) {
        stateReg := State.idle
        pointerReg := 0.U
      }
    }

    is(State.done) {
      fpga.done := true.B
      when (fpga.again) {
        stateReg := State.idle
        pointerReg := 0.U
      }
    }
  }
}

object SPIOffChipBasys3 extends App {
  val basys3ClockFreq = 100000000 // 100MHz
  val spiFreq =         1000000 // 1MHz
  val refreshRate =     100000  // 1ms refresh rate for the seven segment display
  val testCases =      4 // number of test cases to be tested
  (new chisel3.stage.ChiselStage).emitVerilog(new FPGATest(spiFreq, basys3ClockFreq, refreshRate, testCases), Array("--target-dir", "generated"))
}