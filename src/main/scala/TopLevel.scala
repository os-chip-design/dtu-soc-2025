import chisel3._
import wildcat.pipeline.{InstrIO, MemIO, ThreeCats}
import wildcat.Util


class TopLevel(file: Option[String] = None) extends Module {
  val addrWidth = 32
  val gpioPins = 8 // 8?

  val io = IO(new Bundle {
    val uartRx = Input(Bool())
    val uartTx = Output(Bool())

    val gpio_in = Input(UInt(gpioPins.W))
    val gpio_out = Output(UInt(gpioPins.W))
    val gpio_oeb = Output(UInt(gpioPins.W))

    val mem = new NativeMemoryInterface(DATA_WIDTH = 32, ADDR_WIDTH = 9, WMASK_WIDTH = 4)
    val imem = new InstrIO()
  })

  val addressRanges = Seq(
    ("h00000000".U, "h0000000F".U), // Device 0 (UART)
    ("h00000010".U, "h0000001F".U), // Device 1 (SPI)
    ("h00000020".U, "h0000002F".U), // Device 3 (GPIO)
    ("h10000000".U, "h1FFFFFFF".U), // Device 4 (data memory)
  )
  val (memory, start) = file match {
    case Some(file) => Util.getCode(file)
    case None        => (Array.fill(4096)(0), 0) // fallback: zero-initialized memory
  }
  //val imem_testfile = getClass.getResource("/hello.bin").getPath
  val devices = addressRanges.length
  val interconnect = Module(new PipeConInterconnect(memory, addrWidth, devices, addressRanges))
 
  // Modules
  //val cpu = Module(new ThreeCats())
  val uart = Module(new UartModule(9600, 1000))

  // PipeCon peripherals
  val UARTPeripheral = Module(new UARTPeripheral(addrWidth))
  val SPIPeripheral = Module(new SPIPeripheral(addrWidth))
  val GPIOPeripheral = Module(new GPIOPeripheral(addrWidth, gpioPins))
  val NativeMemory2Pipecon = Module(new NativeMemory2Pipecon(DATA_WIDTH = 32, ADDR_WIDTH = 9, WMASK_WIDTH = 4))

  UARTPeripheral.io <> interconnect.io.device(0)
  SPIPeripheral.io <> interconnect.io.device(1)
  GPIOPeripheral.io.mem_ifc <> interconnect.io.device(2)
  NativeMemory2Pipecon.io.pipe <> interconnect.io.device(3)

  // Connections
  SPIPeripheral.testIo.testRdData := DontCare
  UARTPeripheral.testIo.testWrData := DontCare

  // Connect UART module to Caravel IO pins
  uart.io.rx := io.uartRx
  io.uartTx := uart.io.tx

  // TODO: Connect GPIO module to Caravel IO pins
  io.gpio_out := DontCare
  io.gpio_oeb := DontCare

  for (i <- 0 until gpioPins) {
    // GPIOPeripheral.gpio_input(i) := io.gpio_in(i)
    // io.gpio_out(i) := GPIOPeripheral.gpio_output(i)
    // TODO when and where should oeb (direction) be set?
  }

  // TODO: Connect memory to output.
  //NativeMemory2Pipecon.io.native <> io.mem

  // TODO: Connect UART module to UARTPeripheral for data exchange
  uart.io.tx_valid := DontCare
  uart.io.tx_data := DontCare

  // CPU gets instructions from external memory
  //io.imem.address := cpu.io.imem.address
  //cpu.io.imem.data := io.imem.data
  //cpu.io.imem.stall := io.imem.stall
  
  // All CPU memory accesses go through the interconnect
  //interconnect.io.dmem <> cpu.io.dmem

}
