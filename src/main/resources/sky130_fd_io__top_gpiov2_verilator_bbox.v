(* blackbox *)
module sky130_fd_io__top_gpiov2 (
    input  wire        OUT,
    input  wire        OE_N,
    input  wire        HLD_H_N,
    input  wire        ENABLE_H,
    input  wire        ENABLE_INP_H,
    input  wire        ENABLE_VDDA_H,
    input  wire        ENABLE_VSWITCH_H,
    input  wire        ENABLE_VDDIO,
    input  wire        INP_DIS,
    input  wire        IB_MODE_SEL,
    input  wire        VTRIP_SEL,
    input  wire        SLOW,
    input  wire        HLD_OVR,
    input  wire        ANALOG_EN,
    input  wire        ANALOG_SEL,
    input  wire        ANALOG_POL,
    input  wire [2:0]  DM,

    output wire        IN,
    output wire        IN_H,
    output wire        TIE_HI_ESD,

    inout  wire        PAD,
    inout  wire        PAD_A_NOESD_H,
    inout  wire        PAD_A_ESD_0_H,
    inout  wire        PAD_A_ESD_1_H,
    inout  wire        AMUXBUS_A,
    inout  wire        AMUXBUS_B
);
// synthesis syn_black_box
endmodule
