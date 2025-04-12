import chisel3._
import chisel3.util._

class SevenSegDec extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(4.W))
    val out = Output(UInt(7.W))
  })
    io.out := 0.U(7.W)
    switch(io.in) {
      is(0.U) {
        io.out := "b0111111".U //0
      }
      is(1.U) {
        io.out := "b0000110".U //1
      }
      is(2.U) {
        io.out := "b1011011".U //2
      }
      is(3.U) {
        io.out := "b1001111".U //3
      }
      is(4.U) {
        io.out := "b1100110".U //4
      }
      is(5.U) {
        io.out := "b1101101".U //5
      }
      is(6.U) {
        io.out := "b1111101".U //6
      }
      is(7.U) {
        io.out := "b0000111".U //7
      }
      is(8.U) {
        io.out := "b1111111".U //8
      }
      is(9.U) {
        io.out := "b1101111".U //9
      }
      is(10.U) {
        io.out := "b1110111".U //A
      }
      is(11.U) {
        io.out := "b1111100".U //b
      }
      is(12.U) {
        io.out := "b0111001".U //C
      }
      is(13.U) {
        io.out := "b1011110".U //d
      }
      is(14.U) {
        io.out := "b1111001".U //E
      }
      is(15.U) {
        io.out := "b1110001".U //F
      }
    }
}

class DisplayDriver(refreshRate: Int) extends Module {
  val io = IO(new Bundle {
    val input = Input(UInt(16.W))
    val seg = Output(UInt(7.W))
    val an = Output(UInt(4.W))
  })


  val sevSegDecoder = Module(new SevenSegDec())
  val stateReg = RegInit(0.U(2.W))
  val select = WireDefault("b0001".U(4.W))
  val tickCounterReg = RegInit(0.U(32.W))
  val tick = tickCounterReg === refreshRate.U
  sevSegDecoder.io.in := WireDefault("b0001".U(4.W))
  tickCounterReg := tickCounterReg + 1.U
  when(tick) {
    tickCounterReg := 0.U
    stateReg := stateReg + 1.U
  }
  switch(stateReg) {
    is(0.U) {
        select := "b1000".U
        sevSegDecoder.io.in := io.input(15, 12)
    }
    is(1.U) {
        select := "b0100".U
        sevSegDecoder.io.in := io.input(11, 8)
    }
    is(2.U) {
        select := "b0010".U
        sevSegDecoder.io.in := io.input(7, 4)
    }
    is(3.U) {
        select := "b0001".U
        sevSegDecoder.io.in := io.input(3, 0)
    }
  }
  io.seg := ~sevSegDecoder.io.out
  io.an := ~select
}

