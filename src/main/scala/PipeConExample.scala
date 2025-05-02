import chisel3._
import chisel3.util._

class PipeConExample(file: String, addrWidth: Int, devices: Int) extends Module {
  val io = IO(new Bundle {
  })


  val interconnect = Module(new PipeConInterconnect(file, addrWidth, devices))
  val UARTPeripheral = Module(new UARTPeripheral(addrWidth))
  val SPIPeripheral = Module(new SPIPeripheral(addrWidth))
  val GPIOPeripheral = Module(new GPIOPeripheral(addrWidth, 8))

  UARTPeripheral.io <> interconnect.io.device(0)
  SPIPeripheral.io <> interconnect.io.device(1)
  GPIOPeripheral.io.mem_ifc <> interconnect.io.device(2)

  UARTPeripheral.testIo.testWrData := ("hDEADBEEF".U)
  SPIPeripheral.testIo.testRdData := 0.U

}