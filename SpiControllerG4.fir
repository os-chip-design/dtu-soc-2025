circuit SpiControllerG4 :
  module SpiControllerG4 :
    input clock : Clock
    input reset : UInt<1>
    output io : { flip cpuWriteData : UInt<32>, cpuReadData : UInt<32>, flip enable : UInt<1>, flip spiMiso : UInt<1>, spiMosi : UInt<1>, flip spiCs : UInt<1>, spiClk : UInt<1>, flip prescale : UInt<4>}

    reg stateReg : UInt<3>, clock with :
      reset => (reset, UInt<1>("h0")) @[SpiControllerG4.scala 29:27]
    node _CNT_MAX_T = sub(io.prescale, UInt<1>("h1")) @[SpiControllerG4.scala 31:31]
    node CNT_MAX = tail(_CNT_MAX_T, 1) @[SpiControllerG4.scala 31:31]
    reg cntClk : UInt<3>, clock with :
      reset => (reset, UInt<3>("h0")) @[SpiControllerG4.scala 32:25]
    reg spiClkReg : UInt<1>, clock with :
      reset => (reset, UInt<1>("h0")) @[SpiControllerG4.scala 33:28]
    reg spiData : UInt<32>, clock with :
      reset => (reset, UInt<32>("h0")) @[SpiControllerG4.scala 35:26]
    reg txBuffer : UInt<32>, clock with :
      reset => (reset, UInt<32>("h0")) @[SpiControllerG4.scala 36:27]
    reg txShiftReg : UInt<8>, clock with :
      reset => (reset, UInt<8>("h0")) @[SpiControllerG4.scala 37:29]
    reg spiBuffer : UInt<32>, clock with :
      reset => (reset, UInt<32>("h0")) @[SpiControllerG4.scala 39:28]
    reg rxBuffer : UInt<32>, clock with :
      reset => (reset, UInt<32>("h0")) @[SpiControllerG4.scala 40:27]
    reg rxShiftReg : UInt<8>, clock with :
      reset => (reset, UInt<8>("h0")) @[SpiControllerG4.scala 41:29]
    io.cpuReadData <= UInt<1>("h0") @[SpiControllerG4.scala 43:20]
    io.spiMosi <= UInt<1>("h0") @[SpiControllerG4.scala 44:16]
    node _cntClk_T = add(cntClk, UInt<1>("h1")) @[SpiControllerG4.scala 46:22]
    node _cntClk_T_1 = tail(_cntClk_T, 1) @[SpiControllerG4.scala 46:22]
    cntClk <= _cntClk_T_1 @[SpiControllerG4.scala 46:12]
    node _T = eq(cntClk, CNT_MAX) @[SpiControllerG4.scala 47:18]
    when _T : @[SpiControllerG4.scala 47:31]
      cntClk <= UInt<1>("h0") @[SpiControllerG4.scala 48:16]
      node _spiClkReg_T = eq(spiClkReg, UInt<1>("h0")) @[SpiControllerG4.scala 49:22]
      spiClkReg <= _spiClkReg_T @[SpiControllerG4.scala 49:19]
    io.spiClk <= spiClkReg @[SpiControllerG4.scala 51:15]

