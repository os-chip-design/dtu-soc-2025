import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.HasBlackBoxResource
import chisel3.util.HasBlackBoxInline

// Boilerplate stuff - work in progress

// Need to implement the inout pins to be able to test, 
// but even then, no guarantee that it will function...

class sky130_fd_io__top_gpiov2 extends BlackBox with HasBlackBoxResource {
//class GpioPad extends BlackBox with HasBlackBoxInline {
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

    val PAD                 = Analog(1.W)
    val PAD_A_NOESD_H       = Analog(1.W)
    val PAD_A_ESD_0_H       = Analog(1.W)
    val PAD_A_ESD_1_H       = Analog(1.W)
    val AMUXBUS_A           = Analog(1.W)
    val AMUXBUS_B           = Analog(1.W)
    
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
        
    */

    val IN                  = Output(Bool())
    val IN_H                = Output(Bool())
    val TIE_HI_ESD          = Output(Bool())
  })
  //addResource("funct_def.v")
  addResource("sky130_fd_io.v")
  //addResource("../../../soc-chip-2025/dependencies/pdks/sky130B/libs.ref/sky130_fd_io/verilog/sky130_fd_io.v")

  //setInline(
  //  s"""
  //    |`DEFINE FUNCTIONAL
  //  """.stripMargin,
  //  "../../../soc-chip-2025/dependencies/pdks/sky130B/libs.ref/sky130_fd_io/verilog/sky130_fd_io.v")
}