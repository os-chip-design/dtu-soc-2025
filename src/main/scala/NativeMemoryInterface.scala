import chisel3._

class NativeMemoryInterface(DATA_WIDTH: Int = 32, ADDR_WIDTH: Int = 9, WMASK_WIDTH: Int = 4) extends Bundle {
    val address = Output(UInt(ADDR_WIDTH.W))
    val wdata = Output(UInt(DATA_WIDTH.W))
    val rdata = Input(UInt(DATA_WIDTH.W))
    val cs = Output(Bool())
    val wmask = Output(UInt(WMASK_WIDTH.W))     
    val wen = Output(Bool())     
}
