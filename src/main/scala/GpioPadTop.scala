import chisel3._

class GpioPadTop extends Module {
    val io = IO(new Bundle {

    val OUT                 = Input(Bool())
    val OE_N                = Input(Bool())
    val HLD_H_N             = Input(Bool())
    val ENABLE_H            = Input(Bool())
    val ENABLE_INP_H        = Input(Bool())
    val ENABLE_VDDA_H       = Input(Bool())
    val ENABLE_VSWITCH_H    = Input(Bool())
    val ENABLE_VDDIO        = Input(Bool())
    val INP_DIS             = Input(Bool())
    val IB_MODE_SEL         = Input(Bool())
    val VTRIP_SEL           = Input(Bool())
    val SLOW                = Input(Bool())
    val HLD_OVR             = Input(Bool())
    val ANALOG_EN           = Input(Bool())
    val ANALOG_SEL          = Input(Bool())
    val ANALOG_POL          = Input(Bool())
    val DM                  = Input(UInt(3.W))

    /* Pins currently ommited
        inout VDDIO;
        inout VDDIO_Q;
        inout VDDA;
        inout VCCD;
        inout VSWITCH;
        inout VCCHIB;
        inout VSSA;
        inout VSSD;
        inout VSSIO_Q;
        inout VSSIO;
        inout PAD;
        inout PAD_A_NOESD_H,PAD_A_ESD_0_H,PAD_A_ESD_1_H;
        inout AMUXBUS_A;
        inout AMUXBUS_B;
    */

    val IN                  = Output(Bool())
    val IN_H                = Output(Bool())
    val TIE_HI_ESD          = Output(Bool())

    })
    val gpioPad = Module(new GpioPad)
    gpioPad.io.OUT := io.OUT
    gpioPad.io.OE_N := io.OE_N
    gpioPad.io.HLD_H_N := io.HLD_H_N
    gpioPad.io.ENABLE_H := io.ENABLE_H
    gpioPad.io.ENABLE_INP_H := io.ENABLE_INP_H
    gpioPad.io.ENABLE_VDDA_H := io.ENABLE_VDDA_H
    gpioPad.io.ENABLE_VSWITCH_H := io.ENABLE_VSWITCH_H
    gpioPad.io.ENABLE_VDDIO := io.ENABLE_VDDIO
    gpioPad.io.INP_DIS := io.INP_DIS
    gpioPad.io.IB_MODE_SEL := io.IB_MODE_SEL
    gpioPad.io.VTRIP_SEL := io.VTRIP_SEL
    gpioPad.io.SLOW := io.SLOW
    gpioPad.io.HLD_OVR := io.HLD_OVR
    gpioPad.io.ANALOG_EN := io.ANALOG_EN
    gpioPad.io.ANALOG_SEL := io.ANALOG_SEL
    gpioPad.io.ANALOG_POL := io.ANALOG_POL
    gpioPad.io.DM := io.DM
}