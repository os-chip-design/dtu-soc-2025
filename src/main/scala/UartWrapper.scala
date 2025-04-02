import chisel.lib.uart._
import chisel3._
import chisel3.util._

class UartWrapper(freq: Int, baud: Int, txBufferDepth: Int, rxBufferDepth: Int
                 ) extends Module {

  val io = IO(new Bundle{
    //creating a bus line according to the structure of the respond port class
    val bus = Bus.RespondPort()
    //creating rx and tx pins according to the uartmodule structure
    val uart = MemoryMappedUart.UartPins()
    //creating outside pins for the flow control
    val RTS = Output(UInt(1.W))
    val CTS = Input(UInt(1.W))

  })

  val uartMod = Module(new MemoryMappedUart(freq, baud, txBufferDepth, rxBufferDepth))

  //Connecting the pins to the instance of the uartmodule
  io.bus <> uartMod.io.port
  io.uart <> uartMod.io.pins


}


