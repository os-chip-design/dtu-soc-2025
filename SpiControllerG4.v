module SpiControllerG4(
  input         clock,
  input         reset,
  input  [31:0] io_cpuWriteData,
  output [31:0] io_cpuReadData,
  input         io_enable,
  input         io_spiMiso,
  output        io_spiMosi,
  input         io_spiCs,
  output        io_spiClk,
  input  [3:0]  io_prescale
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  wire [3:0] CNT_MAX = io_prescale - 4'h1; // @[SpiControllerG4.scala 31:31]
  reg [2:0] cntClk; // @[SpiControllerG4.scala 32:25]
  reg  spiClkReg; // @[SpiControllerG4.scala 33:28]
  wire [2:0] _cntClk_T_1 = cntClk + 3'h1; // @[SpiControllerG4.scala 46:22]
  wire [3:0] _GEN_2 = {{1'd0}, cntClk}; // @[SpiControllerG4.scala 47:18]
  assign io_cpuReadData = 32'h0; // @[SpiControllerG4.scala 43:20]
  assign io_spiMosi = 1'h0; // @[SpiControllerG4.scala 44:16]
  assign io_spiClk = spiClkReg; // @[SpiControllerG4.scala 51:15]
  always @(posedge clock) begin
    if (reset) begin // @[SpiControllerG4.scala 32:25]
      cntClk <= 3'h0; // @[SpiControllerG4.scala 32:25]
    end else if (_GEN_2 == CNT_MAX) begin // @[SpiControllerG4.scala 47:31]
      cntClk <= 3'h0; // @[SpiControllerG4.scala 48:16]
    end else begin
      cntClk <= _cntClk_T_1; // @[SpiControllerG4.scala 46:12]
    end
    if (reset) begin // @[SpiControllerG4.scala 33:28]
      spiClkReg <= 1'h0; // @[SpiControllerG4.scala 33:28]
    end else if (_GEN_2 == CNT_MAX) begin // @[SpiControllerG4.scala 47:31]
      spiClkReg <= ~spiClkReg; // @[SpiControllerG4.scala 49:19]
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  cntClk = _RAND_0[2:0];
  _RAND_1 = {1{`RANDOM}};
  spiClkReg = _RAND_1[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
