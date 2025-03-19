import chisel3._
import chisel3.util.HasBlackBoxResource

class Gpio_Pad extends BlackBox with HasBlackBoxResource {
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

    val IN                  = Output(Bool())
    val IN_H                = Output(Bool())
    val TIE_HI_ESD          = Output(Bool())
  })
  addResource("../../../soc-chip-2025/dependencies/pdks/sky130B/libs.ref/sky130_fd_io/verilog/sky130_fd_io.v")
}

