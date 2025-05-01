import chisel3._
import chisel3.util._

class PipeConExample(file: String, addrWidth: Int, devices: Int) extends Module {
  val io = IO(new Bundle {
  })


  val interconnect = Module(new PipeConInterconnect(file, addrWidth, devices))
  val UARTPeripheral = Module(new UARTPeripheral(addrWidth))
  val SPIPeripheral = Module(new SPIPeripheral(addrWidth))


  UARTPeripheral.io <> interconnect.io.device(0)
  SPIPeripheral.io <> interconnect.io.device(1)

  UARTPeripheral.testIo.testWrData := 0.U
  SPIPeripheral.testIo.testRdData := 0.U

}