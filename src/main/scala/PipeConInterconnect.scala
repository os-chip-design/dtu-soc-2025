import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import wildcat.pipeline.{InstructionROM, MemIO, ThreeCats}
import wildcat.Util

class PipeConInterconnect(addrWidth: Int) extends Module {
  val io = IO(new Bundle {
    val uart = new PipeCon(addrWidth)
    val SPI  = new PipeCon(addrWidth)
    val rdEnableTest = Output(Bool())
  })

  // === Instantiate CPU and ROM ===
  val cpu = Module(new ThreeCats())

  val testProgram = Array(
    0x00100093, // li x1, 1
    0x00200113, // li x2, 2
    0x0000A183, // lw x3, 0(x1)
    0x00310023, // sw x3, 0(x1) (Store word in x3 to address 0x01, i.e., UART)
    0xDEADBEEF,  // Write 0xDEADBEEF directly to UART address 0x01
    0x0000006F   // Loop
  )

  val paddedProgram = testProgram ++ Array.fill(251)(0x00000013)
  val imem = Module(new InstructionROM(paddedProgram))
  cpu.io.imem <> imem.io

  //cpu.io.imem.stall := false.B
  cpu.io.dmem.stall := false.B
  //cpu.io.imem.data := 0.U

  //printf("Instruction: 0x%x, Write Address: 0x%x, Write Data: 0x%x\n", 
  //  testProgram(3).U, cpu.io.dmem.wrAddress, cpu.io.dmem.wrData)
  printf("Instruction Fetch: 0x%x\n", cpu.io.imem.address)


  // === rdEnable output ===
  io.rdEnableTest := cpu.io.dmem.rdEnable

  // === Instantiate Peripherals ===
  val uartPeripheral = Module(new UARTPeripheral(addrWidth))
  val spiPeripheral  = Module(new SPIPeripheral(addrWidth))

  // === Default peripheral I/Os ===
  uartPeripheral.io.wr := io.uart.wr
  uartPeripheral.io.rd := io.uart.rd
  uartPeripheral.io.wrMask := 0.U
  uartPeripheral.io.wrData := 0.U
  uartPeripheral.io.address := 0.U
  uartPeripheral.testIo.testWrData := io.uart.wrData

  spiPeripheral.io.wr := false.B
  spiPeripheral.io.rd := false.B
  spiPeripheral.io.wrMask := 0.U
  spiPeripheral.io.wrData := 0.U
  spiPeripheral.io.address := 0.U
  spiPeripheral.testIo.testRdData := 0.U

  // === External IO defaults ===
  io.uart.rdData := 0.U
  io.uart.ack    := false.B
  io.SPI.rdData  := 0.U
  io.SPI.ack     := false.B

  // === Address Map ===
  val uartAddress = "h02".U(addrWidth.W)
  val spiAddress  = "h03".U(addrWidth.W)

  // === CPU signals ===
  val isRead  = cpu.io.dmem.rdEnable
  val isWrite = cpu.io.dmem.wrEnable.reduce(_ || _)
  printf("CPU Write Enable: %b\n", isWrite)


  // Register them
  val rdAddrReg = RegNext(cpu.io.dmem.rdAddress)
  val wrAddrReg = RegNext(cpu.io.dmem.wrAddress)
  val wrDataReg = RegNext(cpu.io.dmem.wrData)
  val wrMaskReg = RegNext(cpu.io.dmem.wrEnable.asUInt)

  val doReadReg  = RegNext(isRead)
  val doWriteReg = RegNext(isWrite)

  // Read Data Register
  val rdDataReg = RegInit(0.U(32.W))

  // === Write Logic ===
  printf("Write Address: 0x%x, UART Address: 0x%x, SPI Address: 0x%x\n", 
    cpu.io.dmem.wrAddress, uartAddress, spiAddress)

  when (isWrite) {
    printf("CPU Write Enable: %d, Write Mask: 0x%x\n", isWrite, cpu.io.dmem.wrEnable.asUInt)
    printf("CPU Write Address: 0x%x\n", cpu.io.dmem.wrAddress)
    when (cpu.io.dmem.wrAddress === uartAddress) {
      printf("CPU Writing to UART: Addr = 0x%x, Data = 0x%x\n", 
        cpu.io.dmem.wrAddress, cpu.io.dmem.wrData)
      uartPeripheral.io.wr := true.B
      uartPeripheral.io.address := cpu.io.dmem.wrAddress
      uartPeripheral.io.wrData := cpu.io.dmem.wrData
      uartPeripheral.io.wrMask := cpu.io.dmem.wrEnable.asUInt
    } .elsewhen (cpu.io.dmem.wrAddress === spiAddress) {
      printf("CPU Writing to SPI: Addr = 0x%x, Data = 0x%x\n", 
        cpu.io.dmem.wrAddress, cpu.io.dmem.wrData)
      spiPeripheral.io.wr := true.B
      spiPeripheral.io.address := cpu.io.dmem.wrAddress
      spiPeripheral.io.wrData := cpu.io.dmem.wrData
      spiPeripheral.io.wrMask := cpu.io.dmem.wrEnable.asUInt
    }
  }


  // === Read Logic ===
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

  // === Connect to CPU ===
  cpu.io.dmem.rdData := rdDataReg

  // === Connect external IOs ===
  io.uart.rdData := uartPeripheral.io.rdData
  io.uart.ack    := uartPeripheral.io.ack
  io.SPI.rdData  := spiPeripheral.io.rdData
  io.SPI.ack     := spiPeripheral.io.ack
}
