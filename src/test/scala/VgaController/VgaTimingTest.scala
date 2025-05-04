import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class VgaTimerTest extends AnyFlatSpec with ChiselScalatestTester {

  val H_DISPLAY = 640
  val H_FP = 16
  val H_SYNC = 96
  val H_BP = 48
  val H_TOTAL = H_DISPLAY + H_FP + H_SYNC + H_BP // 800

  val V_DISPLAY = 480
  val V_FP = 10
  val V_SYNC = 2
  val V_BP = 33
  val V_TOTAL = V_DISPLAY + V_FP + V_SYNC + V_BP // 525

  // The places where transitions happen
  val H_SYNC_START = H_DISPLAY + H_FP // 656
  val H_SYNC_END = H_SYNC_START + H_SYNC // 752

  val V_SYNC_START = V_DISPLAY + V_FP // 490
  val V_SYNC_END = V_SYNC_START + V_SYNC // 492

  behavior of "VgaTimer"

  // Combined into single test as it takes ages to run
  it should "produce correct timing signals for 640x480@60Hz" in {
    test(new VgaTimer()).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // This needs to run a ****ton of times so we disable the clock timeout
      dut.clock.setTimeout(0)

      var currentH = 0
      var currentV = 0

      // Simulate for slightly more than one frame to check wrapping
      val totalCyclesToSimulate = H_TOTAL * V_TOTAL + 10 // 420,010 cycles, nice

      // Check initial state (after reset)
      dut.io.hActive.expect(true.B)
      dut.io.vActive.expect(true.B)
      dut.io.hSync.expect(true.B)
      dut.io.vSync.expect(true.B)
      dut.io.pixelX.expect(0.U)
      dut.io.pixelY.expect(0.U)

      // Check that nothing happens when enable is false
      for (cycle <- 1 to 40) {
        dut.io.hActive.expect(true.B)
        dut.io.vActive.expect(true.B)
        dut.io.hSync.expect(true.B)
        dut.io.vSync.expect(true.B)
        dut.io.pixelX.expect(0.U)
        dut.io.pixelY.expect(0.U)
      }

      dut.io.timerEn.poke(true.B)
      // Simulate clock cycles
      for (cycle <- 1 to totalCyclesToSimulate) {
        dut.clock.step()

        // Keep track of expected H/V counts
        val hPrev = currentH
        if (currentH == H_TOTAL - 1) {
          currentH = 0
          if (currentV == V_TOTAL - 1) {
            currentV = 0
          } else {
            currentV += 1
          }
        } else {
          currentH += 1
        }

        // Here we check that the transitions to active display Area
        // and other nice stuff actually happens
        // 1. Active Display Area
        if (currentH < H_DISPLAY && currentV < V_DISPLAY) {
          hPrev >= H_DISPLAY || (currentH == 0 && hPrev != 0) || (currentV != dut.io.pixelY
            .peek()
            .litValue
            .toInt && currentH == 0)
          dut.io.hActive.expect(
            true.B,
            s"Cycle $cycle: hActive should be true at H=$currentH, V=$currentV"
          )
          dut.io.vActive.expect(
            true.B,
            s"Cycle $cycle: vActive should be true at V=$currentV"
          )
          dut.io.hSync.expect(
            true.B,
            s"Cycle $cycle: hSync inactive(1) during active H=$currentH, V=$currentV"
          )
          dut.io.vSync.expect(
            true.B,
            s"Cycle $cycle: vSync inactive(1) during active V=$currentV"
          )
          dut.io.pixelX.expect(
            currentH.U,
            s"Cycle $cycle: pixelX mismatch H=$currentH, V=$currentV"
          )
          dut.io.pixelY.expect(
            currentV.U,
            s"Cycle $cycle: pixelY mismatch H=$currentH, V=$currentV"
          )
        }
        // 2. Horizontal Blanking Period (within Vertical Display)
        else if (currentH >= H_DISPLAY && currentV < V_DISPLAY) {
          dut.io.hActive.expect(
            false.B,
            s"Cycle $cycle: hActive false during H blank H=$currentH, V=$currentV"
          )
          dut.io.vActive
            .expect(true.B, s"Cycle $cycle: vActive true at V=$currentV")
          dut.io.pixelX.expect(currentH.U)
          dut.io.pixelY.expect(currentV.U)

          // Check HSync (Active Low)
          if (currentH >= H_SYNC_START && currentH < H_SYNC_END) {
            if (hPrev < H_SYNC_START)
              dut.io.hSync.expect(
                false.B,
                s"Cycle $cycle: hSync active(0) H=$currentH, V=$currentV"
              )
          } else {
            if (hPrev >= H_SYNC_START && hPrev < H_SYNC_END)
              dut.io.hSync.expect(
                true.B,
                s"Cycle $cycle: hSync inactive(1) H=$currentH, V=$currentV"
              )
          }
        }
        // 3. Vertical Blanking Period
        else if (currentV >= V_DISPLAY) {
          dut.io.hActive.expect(
            currentH < H_DISPLAY,
            s"Cycle $cycle: hActive depends on H even in V blank V=$currentV"
          )
          dut.io.vActive.expect(
            false.B,
            s"Cycle $cycle: vActive false during V blank V=$currentV"
          )

          // Check pixel coordinates - expect the truncated hardware values
          dut.io.pixelX.expect(
            currentH.U,
            s"Cycle $cycle: pixelX mismatch in V blank H=$currentH, V=$currentV"
          ) // X is 10 bits, no truncation issue up to 799

          val expectedPixelY = currentV & 511 // Mask with 2^9 - 1
          dut.io.pixelY.expect(
            expectedPixelY.U,
            s"Cycle $cycle: pixelY mismatch in V blank. Expected $expectedPixelY (truncated) for V=$currentV"
          )
          // Check VSync (Active Low)
          if (currentV >= V_SYNC_START && currentV < V_SYNC_END) {
            dut.io.vSync
              .expect(false.B, s"Cycle $cycle: vSync active(0) V=$currentV")
          } else {
            if (currentV == V_SYNC_END && currentH == 0)
              dut.io.vSync
                .expect(true.B, s"Cycle $cycle: vSync inactive(1) V=$currentV")
          }

          // Check HSync during V blanking
          if (currentH >= H_SYNC_START && currentH < H_SYNC_END) {
            dut.io.hSync.expect(
              false.B,
              s"Cycle $cycle: hSync active(0) H=$currentH during V blank V=$currentV"
            )
          } else {
            dut.io.hSync.expect(
              true.B,
              s"Cycle $cycle: hSync inactive(1) H=$currentH during V blank V=$currentV"
            )
          }
        }

      }
    }
  }
}
