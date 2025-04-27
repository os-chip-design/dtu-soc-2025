import chisel3._
import chisel3.util._
import wildcat.pipeline.ThreeCats

class PipeConInterconnect(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val uart = (new PipeCon(addrWidth))  // Interface for UART
    val SPI = (new PipeCon(addrWidth))   // Interface for SPI
    val rdEnableTest = Output(Bool())
  })


  // === Instantiate the CPU ===
  val cpu = Module(new ThreeCats())

  // === Instantiate Peripherals ===
  val uartPeripheral = Module(new UARTPeripheral(addrWidth))  // Actual UART logic
  val spiPeripheral = Module(new SPIPeripheral(addrWidth))    // Actual SPI logic

  
  if (cpu.io.dmem.rdEnable == true.B) {
    io.rdEnableTest := cpu.io.dmem.rdEnable
  } else if (cpu.io.dmem.rdEnable == false.B) {
    io.rdEnableTest := cpu.io.dmem.rdEnable
  } else {
    io.rdEnableTest := false.B
  }

  // Default for peripherals
  uartPeripheral.io.wr := false.B
  uartPeripheral.io.rd := false.B
  uartPeripheral.io.wrMask := 0.U
  uartPeripheral.io.wrData := 0.U
  uartPeripheral.io.address := 0.U
  uartPeripheral.testIo.testRdData := 0.U
  spiPeripheral.io.wr := false.B
  spiPeripheral.io.rd := false.B
  spiPeripheral.io.wrMask := 0.U
  spiPeripheral.io.wrData := 0.U
  spiPeripheral.io.address := 0.U
  spiPeripheral.testIo.testRdData := 0.U

  // === Setup default CPU IO ===
  cpu.io.imem.stall := false.B
  cpu.io.imem.data := 0.U
  cpu.io.dmem.stall := false.B
  cpu.io.dmem.rdData := 0.U

  // === Defaults for peripherals ===
  io.uart.rdData := 0.U
  io.uart.ack := false.B
  io.SPI.rdData := 0.U
  io.SPI.ack := false.B


  // === Internal: Handling reads/writes ===
  val uartAddress = "h01".U(addrWidth.W)
  val spiAddress  = "h02".U(addrWidth.W)

  val isRead  = cpu.io.dmem.rdEnable
  val isWrite = cpu.io.dmem.wrEnable.reduce(_ || _)

  // --- Register addresses to break comb loops ---
  val rdAddrReg = RegNext(cpu.io.dmem.rdAddress)
  val wrAddrReg = RegNext(cpu.io.dmem.wrAddress)
  val wrDataReg = RegNext(cpu.io.dmem.wrData)
  val wrMaskReg = RegNext(cpu.io.dmem.wrEnable.asUInt)

  val doReadReg  = RegNext(isRead)
  val doWriteReg = RegNext(isWrite)

  // === Read Data Register ===
  val rdDataReg = RegInit(0.U(32.W))

  // === Write logic ===
  when (isWrite) {
    when (cpu.io.dmem.wrAddress === uartAddress) {
      uartPeripheral.io.wr := true.B
      uartPeripheral.io.address := cpu.io.dmem.wrAddress
      uartPeripheral.io.wrData := cpu.io.dmem.wrData
      uartPeripheral.io.wrMask := cpu.io.dmem.wrEnable.asUInt
    } .elsewhen (cpu.io.dmem.wrAddress === spiAddress) {
      spiPeripheral.io.wr := true.B
      spiPeripheral.io.address := cpu.io.dmem.wrAddress
      spiPeripheral.io.wrData := cpu.io.dmem.wrData
      spiPeripheral.io.wrMask := cpu.io.dmem.wrEnable.asUInt
    }
  }

  // === Read logic ===
  when (doReadReg) {
    when (rdAddrReg === uartAddress) {
      uartPeripheral.io.rd := true.B
      uartPeripheral.io.address := rdAddrReg
      rdDataReg := uartPeripheral.io.rdData
    } .elsewhen (rdAddrReg === spiAddress) {
      spiPeripheral.io.rd := true.B
      spiPeripheral.io.address := rdAddrReg
      rdDataReg := spiPeripheral.io.rdData
    } .otherwise {
      rdDataReg := 0.U
    }
  } .otherwise {
    rdDataReg := 0.U
  }

  // === CPU gets the registered read data ===
  cpu.io.dmem.rdData := rdDataReg

  // === Connect the I/O bundle to the UART peripheral ===
  io.uart.rdData := uartPeripheral.io.rdData
  io.uart.ack := uartPeripheral.io.ack
  io.SPI.rdData := spiPeripheral.io.rdData
  io.SPI.ack := spiPeripheral.io.ack

}
