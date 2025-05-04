package SpiControllerG4

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chiseltest.simulator.WriteVcdAnnotation
import chisel3.util._

// to run this write in the terminal: sbt "testOnly SpiControllerG4.SpiControllerTopTest"

class SpiControllerTopTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "SpiControllerTop"
  it should "pass" in { // correctly map registers and control signals
    test(new SpiControllerG4.SpiControllerTop).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Configure transaction parameters
      val sendData = 0xAAL
      val expectData = 0x55

      // 1. Configure Registers
      val txData = sendData
      dut.io.txReg0.poke(txData.U(31, 0)) // Lower 32 bits: 0x9ABCDEF0
      dut.io.txReg1.poke((txData >> 32).U) // Upper 32 bits: 0x12345678
      // Correct configuration for 8-bit send/receive, Mode 0
      def makeControl(
                       recvLen:    Int,
                       waitCycles: Int,
                       sendLen:    Int,
                       prescale:   Int,
                       mode:       Int,
                       enable:     Boolean
                     ): UInt = {
        val raw = (BigInt(if (enable) 1 else 0) << 31) |
          (BigInt(recvLen)            << 20) |
          (BigInt(waitCycles)         << 13) |
          (BigInt(sendLen)            << 6)  |
          (BigInt(prescale)           << 2)  |
          BigInt(mode)
        raw.U(32.W)
      }

      // now just call it with your desired values:
      // enable=1, receiveLength=8, numWaitCycles=0, sendLength=8, prescale=2, mode=0
      val sendLen = 8
      val waitCycles = 0
      val prescale  = 2

      dut.io.controlReg.poke(makeControl(
        recvLen    = 8,
        waitCycles = 0,
        sendLen    = 8,
        prescale   = 2,
        mode       = 0,
        enable     = true
      ))

      // 2. Initial State Check
      dut.clock.step(1)
      println(s"FlagReg: 0x${dut.io.flagReg.peek().litValue.toInt.toHexString}")
      dut.io.flagReg.expect(1.U)  // done=0 (bit 1), ready=1 (bit 0),: binary "01"


      // 3. Start Transaction
      while(!dut.io.spiCs.peek().litToBoolean){
        dut.clock.step(1)
      }  // CS should assert

      // 4. Simulate slave sending 0x55 (8 bits)
      // Slave response simulation
      val misoData = expectData // 8-bit data to send
      var bitCount = 0
      var prevClk = false.B // Chisel Bool type

      // Set first bit before initial clock edge (Mode 0 requirement)
      dut.io.spiMiso.poke(((misoData >> (7 - bitCount)) & 1).B)
      bitCount += 1

      // Process remaining bits
      while (bitCount < 8) {
        val currentClk = dut.io.spiClk.peek().litToBoolean

        // Detect falling edge using proper type conversion
        if (prevClk.litToBoolean && !currentClk) {
          dut.io.spiMiso.poke(((misoData >> (7 - bitCount)) & 1).B)
          bitCount += 1
        }

        // Convert Scala Boolean to Chisel Bool
        prevClk = currentClk.B
        dut.clock.step(1)
      }

      // 5. Wait for completion (flagReg[1] = done)
      while ((dut.io.flagReg.peek().litValue.toInt & 2) == 0) {
        dut.clock.step(1)
      }

      //6. Verify received data
      dut.clock.step(1)
      dut.io.rxReg0.expect((expectData >> (sendLen + waitCycles)).U)
      dut.io.txReg0.expect(sendData.U)
      dut.io.flagReg.expect(1.U) //ready signal
    }
  }
}
