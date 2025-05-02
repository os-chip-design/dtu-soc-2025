import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.io.{File, IOException}

import wildcat.Util
import wildcat.pipeline.ThreeCats

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
  })

  val addressRanges = Seq(
    ("h00000000".U, "h0000000F".U), // Device 0 (UART)
    ("h00000010".U, "h0000001F".U), // Device 1 (SPI)
    ("h00000020".U, "h0000002F".U) // Device 3 (GPIO)
  )

  val (memory, start) = Util.getCode(file)
  val cpu = Module(new ThreeCats())
  // val dmem = Module(new TestRAM(memory))
  val imem = Module(new PipeConMemory(memory))

  val interconnect = Module(new PipeConInterconnect(addrWidth, addressRanges))
  val UARTPeripheral = Module(new UARTPeripheral(addrWidth))
  val SPIPeripheral = Module(new SPIPeripheral(addrWidth))
  val GPIOPeripheral = Module(new GPIOPeripheral(addrWidth, 8))

  imem.io.address := cpu.io.imem.address
  cpu.io.imem.data := imem.io.data
  cpu.io.imem.stall := imem.io.stall
  cpu.io.dmem.stall := false.B
  
  cpu.io.dmem <> interconnect.io.dmem

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

  io.cpuRdAddress := cpu.io.dmem.rdAddress
  io.cpuRdData := cpu.io.dmem.rdData
  io.cpuRdEnable := cpu.io.dmem.rdEnable
  io.cpuWrAddress := cpu.io.dmem.wrAddress
  io.cpuWrData := cpu.io.dmem.wrData
  io.cpuWrEnable := cpu.io.dmem.wrEnable.asUInt //cpu.io.dmem.wrEnable.reduce(_ || _)
  io.cpuStall := cpu.io.dmem.stall

  UARTPeripheral.testIo.testWrData := ("hDEADBEEF".U)
  SPIPeripheral.testIo.testRdData := 0.U
}

class PipeConExampleTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipeConExample" should "svart" in {
    // Path to testfile
    val testfile = getClass.getResource("/hello.bin").getPath

    test(new PipeConExample(testfile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { c =>
      val expected = "HelloWorld".map(_.toByte) // List of ASCII bytes
      var idx = 0 // To track the index in the expected data

      c.clock.setTimeout(0)

      // Run for a fixed number of cycles (e.g., 100 cycles)
      for (_ <- 0 until 100) {
        // If wr signal is high, check if the received data matches the expected data at index idx
        c.clock.step(1)
      }
    }
  }

  "PipeConTest" should "instantiate correctly and write to UART" in {

    // Path to testfile
    val testfile = getClass.getResource("/hello.bin").getPath

    test(new PipeConExample(testfile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { c =>
      val expected = "HelloWorld".map(_.toByte) // List of ASCII bytes
      var idx = 0 // To track the index in the expected data

      c.clock.setTimeout(0)

      // Run for a fixed number of cycles (e.g., 100 cycles)
      for (_ <- 0 until 100) {
        // If wr signal is high, check if the received data matches the expected data at index idx
        if (c.io.cpuWrEnable.peek().litValue != 0) {
          val data = c.io.uart_wrData.peek().litValue.toByte
          assert(data == expected(idx),
            s"Test failed at index $idx: expected '${expected(idx).toChar}', got '${data.toChar}'")

          idx += 1 // Move to the next expected character if matched

          if (idx >= expected.length) {
            // If we've matched the whole expected string, loop back to the start
            idx = 0
          }
        }

        // Step the clock
        c.clock.step(1)
      }

      // If everything has passed, the test will just complete successfully
      assert(true, "Test completed without fatal errors.")
    }
  }

  "PipeConTest" should "fail when the expected value is not HelloWorld" in {
    // Path to testfile
    val testfile = getClass.getResource("/hello.bin").getPath

    test(new PipeConExample(testfile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { c =>
      val expected = "HelloWorl".map(_.toByte) // Incorrect expected value (without 'd')
      var idx = 0 // To track the index in the expected data

      c.clock.setTimeout(0)

      // Run for a fixed number of cycles (e.g., 100 cycles)
      var testFailed = false
      for (_ <- 0 until 100) {
        // If wr signal is high, check if the received data matches the expected data at index idx
        if (c.io.cpuWrEnable.peek().litValue != 0) {
          val data = c.io.uart_wrData.peek().litValue.toByte

          if (data != expected(idx)) {
            testFailed = true
            //println(s"Test failed (intentionally) at index $idx: expected '${expected(idx).toChar}', got '${data.toChar}'")
          }

          idx += 1 // Move to the next expected character if matched

          if (idx >= expected.length) {
            // If we've matched the whole expected string, loop back to the start
            idx = 0
          }
        }

        // Step the clock
        c.clock.step(1)
      }

      // Assert that the test failed (because the expected and received data do not match)
      assert(testFailed, "Test did not fail as expected when expected value was incorrect.")
    }
  }

  "PipeConTest" should "run poll.bin and write to UART when finished" in {
    // Load the binary file from test resources
    val testfile = getClass.getResource("/poll.bin").getPath

    test(new PipeConExample(testfile, addrWidth = 32)).withAnnotations(Seq(WriteVcdAnnotation, IcarusBackendAnnotation)) { c =>
      c.clock.setTimeout(0)

      val maxCycles = 100
      println(s"Running $maxCycles cycles...")

      for (cycle <- 0 until maxCycles) {
        // Simulate one clock step
        c.clock.step(1)

        // UART output
        if (c.io.uart_wr.peek().litToBoolean) {
          val uartChar = c.io.uart_wrData.peek().litValue.toByte.toChar
          //println(s"[UART] Wrote: '$uartChar'")
        }

        // Memory reads (simulate address polling)
        if (c.io.cpuRdEnable.peek().litValue != 0) {
          val readAddr = c.io.cpuRdAddress.peek().litValue
          val readData = c.io.cpuRdData.peek().litValue
          println(f"[CPU] Read from 0x$readAddr%08X: 0x$readData%08X")
        }
      }
    }
  }
}

