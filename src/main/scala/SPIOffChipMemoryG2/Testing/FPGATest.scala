import chisel3._
import chisel3.util._

///////////////////// Pin guide /////////////////////
// --- Transition inputs --- (fpga_start, fpga_again, fpga_justRead, fpga_jedec)
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
    val clockDivision: Int = 2, // SPI clock division factor
    val refreshRate: Int = 1000000, // for showing the data on the seven segment display
    val testCases: Int = 4,
    val printTestCases: Boolean = false, // print the test cases to the console
    val endOnError: Boolean = false, // end the simulation on error
    val randomData: Boolean = false, // generate random test cases
    val randomAddresses: Boolean = false // generate random addresses
) extends Module {
  
  val spiPort = IO(new spiIO)

  val fpga = IO(new Bundle {

    val start = Input(Bool())
    val again = Input(Bool()) 
    val jedec = Input(Bool())
    val justRead = Input(Bool())
    val justWrite = Input(Bool())
    val error = Output(Bool())
    val done  = Output(Bool())

    val ramSelect = Output(Bool())
    val targetFlash = Input(Bool())

    val sel = Input(UInt(3.W))
    val seg = Output(UInt(7.W))
    val an  = Output(UInt(4.W))

    val states = Output(UInt(3.W))

    val clear = Input(Bool()) // clear the memory
  })
  val clockWidth = 8
  val dataWidth = 32
  object State extends ChiselEnum {
    val idle, jedec, reading, writing, justRead0, justRead1, justRead2, justWrite0, justWrite1, justWrite2, error, clearing, done = Value        
  }

  val rand: scala.util.Random = {
    val r = new scala.util.Random()
    r.setSeed(0) // set the seed to 0 for reproducibility
    r
  }
  
  //val expectedJEDEC = "h00EF7018".U // table 8.1.1
  val expectedJEDEC = "h180EF700".U // above reversed

  // Registers
  val stateReg = RegInit(State.idle)

  fpga.states := stateReg.asUInt

  val bridge = Module(new Bridge(clockWidth,dataWidth))


  bridge.pipeCon.rd := false.B
  bridge.pipeCon.wr := false.B
  bridge.pipeCon.wrMask := "b1111".U

  bridge.config.jedec := false.B
  bridge.config.clear := false.B
  bridge.config.clockDivision := clockDivision.U
  bridge.config.mode := false.B // SPI clock mode, 0 (indicated by 0) or 3 (indicated by 1)
  bridge.config.targetFlash := fpga.targetFlash // target the flash memory or the RAM
  spiPort <> bridge.spiPort
  
  when (!fpga.targetFlash) {
    spiPort.chipSelect := true.B // chip select for the flash memory
    fpga.ramSelect := bridge.spiPort.chipSelect // chip select for the RAM
  }.otherwise {
    fpga.ramSelect := true.B // chip select for the RAM
  }
  
  val readData = bridge.pipeCon.rdData

  val lastValidData = RegInit(0.U(32.W))
  when (bridge.pipeCon.ack) {
    lastValidData := readData
  }

  val pointerReg = RegInit(0.U(32.W))
  val data = VecInit(Seq.fill(testCases)(0.U(32.W))) 
  for (i <- 0 until testCases) {
    if (randomData) {
      val ran = BigInt(32, rand).toLong
      data(i) := ran.U(32.W)
      if (printTestCases) {
        println(f"data($i) = 0x${ran}%08X")
      }
    } else {
      data(i) := (i).U(32.W)
      if (printTestCases) {
        println(f"data($i) = 0x${(i).toLong}%08X")
      }
    }
  }

  val addresses = VecInit(Seq.fill(testCases)(0.U(24.W))) 
  for (i <- 0 until testCases) {
    if (randomAddresses) {
      val ran = BigInt(24, rand).toLong
      addresses(i) := (ran).U(24.W)
      if (printTestCases) {
        println(f"addresses($i) = 0x${ran}%06X")
      }
    } else {
      addresses(i) := (i << 2).U(24.W) 
      if (printTestCases) {
        println(f"addresses($i) = 0x${(i << 2).toLong}%06X")
      }
    }
  }
  
  
  bridge.pipeCon.address := addresses(pointerReg)
  bridge.pipeCon.wrData  := data(pointerReg)

  val displayDriver = Module(new DisplayDriver(refreshRate))
  fpga.seg := displayDriver.io.seg
  fpga.an  := displayDriver.io.an
  displayDriver.io.input := "hABBA".U // default value for the display
  switch (fpga.sel) {
    is(0.U) { displayDriver.io.input := lastValidData(15, 0) }                     // 000
    is(1.U) { displayDriver.io.input := lastValidData(31, 16) }                    // 001
    is(2.U) { displayDriver.io.input := (data(pointerReg))(15, 0) }             // 010
    is(3.U) { displayDriver.io.input := (data(pointerReg))(31, 16) }            // 011
    is(4.U) { displayDriver.io.input := (addresses(pointerReg))(15, 0) }        // 100
    is(5.U) { displayDriver.io.input := (addresses(pointerReg))(23, 16) }       // 101
    is(6.U) { displayDriver.io.input := pointerReg(15, 0) }                   // 110
    is(7.U) { displayDriver.io.input := pointerReg(31, 16) }                  // 111
  }

  fpga.done := false.B
  fpga.error := false.B

  switch(stateReg) {
    is(State.idle) {
      when (fpga.clear) {
        stateReg := State.clearing
        bridge.pipeCon.wr := true.B
      }.elsewhen (fpga.jedec) {
        stateReg := State.jedec
        bridge.pipeCon.rd := true.B
      }.elsewhen (fpga.justRead) {
        stateReg := State.justRead0
        bridge.pipeCon.rd := true.B
      }.elsewhen (fpga.justWrite) {
        stateReg := State.justWrite0
        bridge.pipeCon.wr := true.B
      }.elsewhen (fpga.start) {
        stateReg := State.writing
        bridge.pipeCon.wr := true.B
      }
    }

    is(State.clearing) {
      when (bridge.pipeCon.ack) {
        stateReg := State.done
      }
      bridge.config.clear := true.B
    }

    is(State.jedec) {
      when (bridge.pipeCon.ack) {
        when (readData =/= expectedJEDEC) {
          stateReg := State.error
        }.otherwise {
          stateReg := State.done
        }
      }
      bridge.config.jedec := true.B      
    }

    is(State.writing) {
      when (bridge.pipeCon.ack) {
        stateReg := State.reading
        bridge.pipeCon.rd := true.B // we need to read next cycle
      }
    }

    is(State.reading) {
      when (bridge.pipeCon.ack) {
        when (readData =/= data(pointerReg)) {
          if (endOnError) {
            stateReg := State.error
          } else {
            fpga.error := true.B
            pointerReg := pointerReg + 1.U
            bridge.pipeCon.address := addresses(pointerReg + 1.U)
            bridge.pipeCon.wrData  := data(pointerReg + 1.U)
            bridge.pipeCon.wr := true.B // we need to write next cycle
            stateReg := State.writing
          }
        }.otherwise {
          when (pointerReg === (testCases - 1).U) {
            stateReg := State.done
          }.otherwise {
            pointerReg := pointerReg + 1.U
            bridge.pipeCon.address := addresses(pointerReg + 1.U)
            bridge.pipeCon.wrData  := data(pointerReg + 1.U)
            bridge.pipeCon.wr := true.B // we need to write next cycle
            stateReg := State.writing
          }
        }
      }
    }


    is(State.justRead0) {
      when (bridge.pipeCon.ack) {
        stateReg := State.justRead1
      }
      when (readData =/= data(pointerReg)) {
        fpga.error := true.B
      }
    }

    is(State.justRead1) {
      when (!fpga.justRead) {
        stateReg := State.done
      }.elsewhen (fpga.again) {
        stateReg := State.justRead2
        when (pointerReg === (testCases - 1).U) {
          pointerReg := 0.U
        }.otherwise {
          pointerReg := pointerReg + 1.U
        }
      }
      when (readData =/= data(pointerReg)) {
        fpga.error := true.B
      }
    }

    is(State.justRead2) {
      when (fpga.start) {
        stateReg := State.justRead0
        bridge.pipeCon.rd := true.B // we need to read next cycle
      }
    }

    is (State.justWrite0) {
      when (bridge.pipeCon.ack) {
        stateReg := State.justWrite1
      }
    }

    is (State.justWrite1) {
      when (!fpga.justWrite) {
        stateReg := State.done
      }.elsewhen (fpga.again) {
        stateReg := State.justWrite2
        when (pointerReg === (testCases - 1).U) {
          pointerReg := 0.U
        }.otherwise {
          pointerReg := pointerReg + 1.U
        }
      }
    }

    is (State.justWrite2) {
      when (fpga.start) {
        stateReg := State.justWrite0
        bridge.pipeCon.wr := true.B // we need to write next cycle
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
  val clockDivision = basys3ClockFreq / spiFreq / 2 // SPI clock division factor divided by 2 as there is a positive edge and a negative edge
  val refreshRate =     100000  // 1ms refresh rate for the seven segment display
  val testCases =      300 // number of test cases to be tested
  (new chisel3.stage.ChiselStage).emitVerilog(new FPGATest(clockDivision, refreshRate, testCases, true, true, true, false), Array("--target-dir", "generated"))
}