import chisel3._

class AutoBaud() extends Module {
  val io = IO(new Bundle {
    val rx = Input(Bool())
    var baudOut = Output(UInt(32.W))
  })
  val prevRx = RegNext(io.rx)
  val edgeCount = RegInit(0.U(32.W))
  val clockCount = RegInit(1.U(32.W))
  val done = RegInit(false.B)
  val startCount = RegInit(false.B)
  val frameLength = 11.U
  val clockFreq = 100000.U
  val baudRate = RegInit(0.U(32.W))

  io.baudOut := 0.U

  when(!done){

    when (prevRx =/= io.rx){
      edgeCount := edgeCount + 1.U
      startCount := true.B
      //printf(p"clock count is : $clockCount \n")
      //printf(p"edgeCount is: $edgeCount \n")
    }

    when(startCount){
      clockCount := clockCount + 1.U
    }


    when (edgeCount === frameLength && prevRx =/= io.rx){
      done := true.B
      startCount := false.B
      baudRate := (clockFreq * (edgeCount)) / (clockCount)
      io.baudOut := baudRate
    }

  }
  when(done){
    io.baudOut := baudRate
  }

}