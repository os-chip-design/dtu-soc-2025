import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3._

class FPGATestSpec extends AnyFlatSpec with ChiselScalatestTester {
  val toPrint = false

  "FPGATest" should "work" in {
    test(new FPGATest(10, 100, 4, toPrint)) { dut =>
    // Quick test mainly to use with waveform
    // testOnly FPGATestSpec -- -DwriteVcd=1
        val clockCycles = 1000
        dut.clock.setTimeout(clockCycles + 1)
        var cnt = 0
        dut.spiPort.dataIn.poke(1.U)
        dut.fpga.jedec.poke(false.B)
        dut.fpga.start.poke(true.B)
        dut.fpga.justRead.poke(false.B)
        dut.fpga.again.poke(false.B)
        dut.fpga.clear.poke(false.B)
        dut.fpga.sel.poke(0.U)
        

        var previousChipSelect = false
        var previousSpiClk = false
        var collectedBits = Seq[Int]()



        while (cnt < clockCycles) {
          cnt += 1
          dut.clock.step(1)

          val currentChipSelect = dut.spiPort.chipSelect.peek().litValue == 0 // Assuming chipSelect is active low
          val currentSpiClk = dut.spiPort.spiClk.peek().litToBoolean

          // Sample dataOut only on the rising edge of spiClk
          if (currentSpiClk && !previousSpiClk && currentChipSelect) {
            val dataOutValue = dut.spiPort.dataOut.peek().litValue
            collectedBits = collectedBits :+ dataOutValue.toInt
          }

          // Print collected bits when chipSelect goes high again
          if (!currentChipSelect && previousChipSelect) {
            if (toPrint) {
              val collectedBitsString = collectedBits.map(_.toString).mkString("")
              val collectedBitsInt = BigInt(collectedBitsString, 2)
              val hexLength = (collectedBitsString.length + 3) / 4 // Calculate the required hex length
              val collectedBitsHex = collectedBitsInt.toString(16).toUpperCase.reverse.padTo(hexLength, '0').reverse
              println(s"Collected bits in binary: $collectedBitsString")
              println(s"Collected bits in hex: 0x$collectedBitsHex")
            }
            collectedBits = Seq[Int]() // Clear the collected bits
          }


          previousChipSelect = currentChipSelect
          previousSpiClk = currentSpiClk
        }
    }
  }
}