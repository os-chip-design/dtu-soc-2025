import chisel3._
import chisel3.util._
import wildcat.pipeline._
import wildcat.Util


class PipeConExample(file: Option[String] = None, addrWidth: Int) extends Module {
  val io = IO(new Bundle { // should be empty when not testing
    val cpuRdAddress = Output(UInt(32.W))
    val cpuRdData = Output(UInt(32.W))
    val cpuRdEnable = Output(Bool())
    val cpuWrAddress = Output(UInt(32.W))
    val cpuWrData = Output(UInt(32.W))
    val cpuWrEnable = Output(UInt(4.W))
    val cpuStall = Output(Bool())
    val uart_address = Output(UInt(addrWidth.W)) 
    val uart_rd = Output(Bool()) 
    val uart_wr = Output(Bool()) 
    val uart_rdData = Output(UInt(32.W)) 
    val uart_wrData = Output(UInt(32.W)) 
    val uart_wrMask = Output(UInt(4.W)) 
    val uart_ack = Output(Bool()) 
    val spi_address = Output(UInt(addrWidth.W)) 
    val spi_rd = Output(Bool()) 
    val spi_wr = Output(Bool()) 
    val spi_rdData = Output(UInt(32.W)) 
    val spi_wrData = Output(UInt(32.W)) 
    val spi_wrMask = Output(UInt(4.W)) 
    val spi_ack = Output(Bool()) 
    val GPIO_address = Output(UInt(addrWidth.W)) 
    val GPIO_rd = Output(Bool()) 
    val GPIO_wr = Output(Bool()) 
    val GPIO_rdData = Output(UInt(32.W)) 
    val GPIO_wrData = Output(UInt(32.W)) 
    val GPIO_wrMask = Output(UInt(4.W)) 
    val GPIO_ack = Output(Bool()) 
  })

  val addressRanges = Seq(
    ("h00000000".U, "h0000000F".U),  // Device 0 (UART)
    ("h00000010".U, "h0000001F".U),  // Device 1 (SPI)
    ("h00000020".U, "h0000002F".U)   // Device 2 (GPIO)
  )
  val (memory, start) = file match {
    case Some(file) => Util.getCode(file)
    case None        => (Array.fill(4096)(0), 0) // fallback: zero-initialized memory
  }
  val cpu = Module(new ThreeCats())
  val dmem = Module(new PipeConMem(memory))
  val imem = Module(new PipeConMemory(memory))
  val devices = addressRanges.length
  val interconnect = Module(new PipeConInterconnect(addrWidth, devices, addressRanges))
  val UARTPeripheral = Module(new UARTPeripheral(addrWidth))
  val SPIPeripheral = Module(new SPIPeripheral(addrWidth))
  val GPIOPeripheral = Module(new GPIOPeripheral(addrWidth, 8)) //8?
  
  interconnect.io.dmem <> cpu.io.dmem
  cpu.io.dmem <> dmem.io
  imem.io.address := cpu.io.imem.address
  val globalStall = interconnect.io.dmem.stall || imem.io.stall
  cpu.io.imem.stall := globalStall
  cpu.io.dmem.stall := interconnect.io.dmem.stall
  cpu.io.imem.data := imem.io.data



  UARTPeripheral.io <> interconnect.io.device(0)
  SPIPeripheral.io <> interconnect.io.device(1)
  GPIOPeripheral.io.mem_ifc <> interconnect.io.device(2)

  // Init test signals (only required for testing)
  io.uart_address := UARTPeripheral.io.address
  io.uart_rd := UARTPeripheral.io.rd
  io.uart_wr := UARTPeripheral.io.wr
  io.uart_rdData := UARTPeripheral.io.rdData
  io.uart_wrData := UARTPeripheral.io.wrData
  io.uart_wrMask := UARTPeripheral.io.wrMask
  io.uart_ack := UARTPeripheral.io.ack

  io.spi_address := SPIPeripheral.io.address
  io.spi_rd := SPIPeripheral.io.rd
  io.spi_wr := SPIPeripheral.io.wr
  io.spi_rdData := SPIPeripheral.io.rdData
  io.spi_wrData := SPIPeripheral.io.wrData
  io.spi_wrMask := SPIPeripheral.io.wrMask
  io.spi_ack := SPIPeripheral.io.ack

  io.GPIO_address := GPIOPeripheral.io.mem_ifc.address
  io.GPIO_rd := GPIOPeripheral.io.mem_ifc.rd
  io.GPIO_wr := GPIOPeripheral.io.mem_ifc.wr
  io.GPIO_rdData := GPIOPeripheral.io.mem_ifc.rdData
  io.GPIO_wrData := GPIOPeripheral.io.mem_ifc.wrData
  io.GPIO_wrMask := GPIOPeripheral.io.mem_ifc.wrMask
  io.GPIO_ack := GPIOPeripheral.io.mem_ifc.ack

  io.cpuRdAddress := cpu.io.dmem.rdAddress
  io.cpuRdData := cpu.io.dmem.rdData
  io.cpuRdEnable := cpu.io.dmem.rdEnable
  io.cpuWrAddress := cpu.io.dmem.wrAddress
  io.cpuWrData := cpu.io.dmem.wrData
  io.cpuWrEnable := cpu.io.dmem.wrEnable.asUInt
  io.cpuStall := cpu.io.dmem.stall

  UARTPeripheral.testIo.testWrData := ("hDEADBEEF".U)
  SPIPeripheral.testIo.testRdData := 0.U

}
object PipeConExampleMain extends App {
  // Example: No file input, default memory used
  val addrWidth = 32
  (new chisel3.stage.ChiselStage).emitVerilog(new PipeConExample(None, addrWidth))
}