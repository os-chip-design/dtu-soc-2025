import chisel3._
import chisel3.util._

class PipeConExample(file: String, addrWidth: Int) extends Module {
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
  val devices = addressRanges.length
  val interconnect = Module(new PipeConInterconnect(file, addrWidth, devices, addressRanges))
  val UARTPeripheral = Module(new UARTPeripheral(addrWidth))
  val SPIPeripheral = Module(new SPIPeripheral(addrWidth))
  val GPIOPeripheral = Module(new GPIOPeripheral(addrWidth, 8)) //8?

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

  io.cpuRdAddress := interconnect.io.cpuRdAddress
  io.cpuRdData := interconnect.io.cpuRdData
  io.cpuRdEnable := interconnect.io.cpuRdEnable
  io.cpuWrAddress := interconnect.io.cpuWrAddress
  io.cpuWrData := interconnect.io.cpuWrData
  io.cpuWrEnable := interconnect.io.cpuWrEnable
  io.cpuStall := interconnect.io.cpuStall

  UARTPeripheral.testIo.testWrData := ("hDEADBEEF".U)
  SPIPeripheral.testIo.testRdData := 0.U

}