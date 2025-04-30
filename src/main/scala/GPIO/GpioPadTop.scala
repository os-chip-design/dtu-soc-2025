import chisel3._

// Wrapper for the black box... Is needed for chisel to be able to test
class GpioPadTop extends Module {
    val io = IO(new Bundle {
        val OUT               = Input(Bool())
        val OE_N              = Input(Bool())
        val IN                = Output(Bool())

        // to be implemented:
        // need to integrate the pullup/down and open drain enabling functionality with the gpio pad
        /*
        val PULLUP_EN         = Input(Bool())
        val PULLDOWN_EN       = Input(Bool())
        val OPEN_DRAIN_EN     = Input(Bool())
        val DRIVE_STRENGTH    = Input(UInt(2.W)) // need to assess what value io cell expects from "drive strength"
        */
        val drivestrength = Input(UInt(2.W)) // drive strength
        val pullup_en = Input(Bool())           // pullup enable
        val pulldown_en = Input(Bool())         // pulldown enable
        val opendrain_en = Input(Bool())        // open drain enable
    })

    // Instantiate the black box gpio module
    val gpioPad             = Module(new GpioPad)

    // Pads we want to expose to other modules
    gpioPad.io.OUT                  := io.OUT
    gpioPad.io.OE_N                 := io.OE_N
    io.IN                           := gpioPad.io.IN
    gpioPad.io.DM                   := io.drivestrength



    // Lets hardcore the rest configurations for now
    // Moving the complexity from the test to the module instead:

    // Enable input buffer during reset
    // (dont care about current consumption atm)
    gpioPad.io.ENABLE_INP_H         := true.B
    
    // Pumped voltage domain.. what is it? Lets just enable atm
    gpioPad.io.ENABLE_VSWITCH_H     := true.B

    // Enable input buffer
    gpioPad.io.ENABLE_H             := true.B    
    gpioPad.io.INP_DIS              := false.B

    // Use VDDIO for voltage thresholds
    gpioPad.io.IB_MODE_SEL          := false.B
    gpioPad.io.ENABLE_VDDIO         := true.B    

    // Set input buffer to CMOS voltage lvls, i.e. 30%/70%.
    gpioPad.io.VTRIP_SEL            := false.B

    // Set hold override to normal mode (i.e. non latched)
    gpioPad.io.HLD_OVR              := true.B
    gpioPad.io.HLD_H_N              := true.B

    // Set analog stuff to off
    gpioPad.io.ENABLE_VDDA_H        := true.B
    gpioPad.io.ANALOG_EN            := false.B
    gpioPad.io.ANALOG_SEL           := false.B
    gpioPad.io.ANALOG_POL           := false.B

    // Set drive mode to "strong" and "slow"
 // gpioPad.io.DM                   := 3.U
 // gpioPad.io.OE_N                 := false.B
    gpioPad.io.SLOW                 := false.B
}