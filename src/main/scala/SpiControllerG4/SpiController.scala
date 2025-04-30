import chisel3._
import chisel3.util._

class SpiController extends Module {
    val io = IO(new Bundle {
        val txData = Input(UInt(64.W))
        val rxData = Output(UInt(64.W))

        val enable = Input(Bool())

        val sendLength = Input(UInt(9.W))
        val numWaitCycles = Input(UInt(9.W))
        val receiveLength = Input(UInt(9.W))

        val spiMiso = Input(Bool())
        val spiMosi = Output(Bool())
        val spiCs = Output(Bool())
        val spiClk = Output(Bool())

        val ready = Output(Bool())
        val done = Output(Bool())

        val prescale = Input(UInt(5.W))
    })

    object State extends ChiselEnum {
        val idle, loadData, active, deassertCS = Value
    }

    import State._
    val stateReg = RegInit(idle)

    val spiDataOut = RegInit(0.U(32.W))
    io.rxData := spiDataOut

    val txShiftReg = RegInit(0.U(64.W))
    val rxShiftReg = RegInit(0.U(32.W))
    val bitCounter = RegInit(0.U(8.W))

    val csReg = RegInit(true.B)
    io.spiCs := csReg

    io.spiMosi := txShiftReg(63)

    val clockEnable = RegInit(false.B)
    val CNT_MAX = (1.U << io.prescale)
    val cntClk = RegInit(0.U(33.W))
    val spiClkReg = RegInit(false.B)

    when (clockEnable) {
        when (io.prescale =/= 0.U) {
            cntClk := cntClk + 1.U
            when (cntClk === CNT_MAX) {
                cntClk := 0.U
                spiClkReg := !spiClkReg
            }
        } .otherwise {
            spiClkReg := false.B
        }
    }

    io.spiClk := spiClkReg

    val spiClkRegPrev = RegNext(spiClkReg)
    val fallingEdge = !spiClkReg && spiClkRegPrev
    val risingEdge = spiClkReg && !spiClkRegPrev

    val totalCycles = Wire(UInt(10.W))
    totalCycles := io.sendLength + io.receiveLength + io.numWaitCycles - 0.U

    io.rxData := spiDataOut

    switch (stateReg) {
        is (idle) {
            when (io.enable) {
                stateReg := loadData
            }
        }
        is (loadData) {
            csReg := false.B
            clockEnable := true.B
            txShiftReg := io.txData << (64.U - io.sendLength)
            //txShiftReg := io.txData
            bitCounter := totalCycles - 1.U
            stateReg := active
        }
        is (active) {

            when (fallingEdge) {
                txShiftReg := txShiftReg << 1
                bitCounter := bitCounter - 1.U
                when (bitCounter === 0.U) {
                    stateReg := deassertCS
                } 
            }

            when (risingEdge) {
                rxShiftReg := (rxShiftReg << 1) | io.spiMiso
            }

        }
        is (deassertCS) {
            spiDataOut := rxShiftReg
            csReg := true.B
            clockEnable := false.B
            stateReg := idle
        }
    }


    when (stateReg === idle) {
        io.ready := true.B
    } .otherwise {
        io.ready := false.B
    }

    when (stateReg === deassertCS) {
        io.done := true.B
    } .otherwise {
        io.done := false.B
    }
}

object SpiMem extends App {
    (new chisel3.stage.ChiselStage).emitVerilog(new SpiMem())
}