import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// Boilerplate stuff - work in progress

class GpioPadTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "The GpioPad"

    it should "pass" in {
        test(new GpioPad()) {
            dut => {
                
                // Enable input buffer during reset 
                // (dont care about current consumption atm)
                dut.io.ENABLE_INP_H.poke(true.B)
                
                // Pumped voltage domain.. what is it? Lets just enable atm
                dut.io.ENABLE_VSWITCH_H.poke(true.B)
                
                // Enable input buffer
                dut.io.ENABLE_H.poke(true.B)
                dut.io.INP_DIS.poke(false.B)

                // Use VDDIO for voltage thresholds
                dut.io.IB_MODE_SEL.poke(false.B)
                dut.io.ENABLE_VDDIO.poke(true.B)

                // Set input buffer to CMOS voltage lvls, i.e. 30%/70%.
                dut.io.VTRIP_SEL.poke(false.B)

                // Set hold override to normal mode (i.e. non latched)
                dut.io.HLD_OVR.poke(true.B)
                dut.io.HLD_H_N.poke(true.B)

                // Set analog stuff to off
                dut.io.ENABLE_VDDA_H.poke(true.B)
                dut.io.ANALOG_EN.poke(false.B)
                dut.io.ANALOG_SEL.poke(false.B)
                dut.io.ANALOG_POL.poke(false.B)

                // Set drive mode to "strong" and "slow"
                dut.io.DM.poke(3.U)
                dut.io.OE_N.poke(false.B)
                dut.io.SLOW.poke(false.B)

                // Give it some stimuli:
                dut.io.OUT.poke(true.B)

                // Or check the input:
                dut.io.IN.expect(false.T)
                
                // Ignore the other pads...
                //dut.io.IN_H.expect(false.B)
                //dut.io.TIE_LO_ESD.expect(false.B)
                //dut.io.TIE_HI_ESD.expect(false.B)

                // Need to implement the inout stuff such as "PAD", i.e.
                //dut.io.PAD.expect(false.B)
            }
        }
    }
}