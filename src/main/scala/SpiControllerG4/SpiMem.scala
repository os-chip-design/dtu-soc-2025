import chisel3._
import chisel3.util._

class SpiMem extends Module {
    val io = IO(new Bundle {
        val cpuCmd = Input(UInt(8.W))
        val cpuWriteData = Input(UInt(24.W))
        val cpuReadData = Output(UInt(32.W))
        val enable = Input(Bool())

        val sendLength = Input(UInt(8.W))
        val numWaitCycles = Input(UInt(8.W))
        val receiveLength = Input(UInt(8.W))

        val spiMiso = Input(Bool())
        val spiMosi = Output(Bool())
        val spiCs = Output(Bool())
        val spiClk = Output(Bool())

        val ready = Output(Bool())
        val done = Output(Bool())

        //val prescale = Input(UInt(4.W))

        val CS0 = Output(Bool())
        val CS2 = Output(Bool())

        //7 seg display
        val seg = Output(UInt(7.W))
        val an = Output(UInt(4.W))

        // leds for testing
        val leds = Output(UInt(8.W))
    })

    object State extends ChiselEnum {
        val idle, loadData, active, deassertCS = Value
    }

    import State._
    val stateReg = RegInit(idle)

    io.CS0 := true.B
    io.CS2 := true.B

    val spiDataOut = RegInit(0.U(32.W))
    io.cpuReadData := spiDataOut

    /*val display = Module(new SevenSegmentDisplay)
    io.seg := display.io.seg
    io.an := display.io.an
    display.io.hexDigits := VecInit(io.cpuReadData(15, 12), io.cpuReadData(11, 8), io.cpuReadData(7, 4), io.cpuReadData(3, 0))*/

    /*val display0 = Module(new SevSegDec)
    io.seg*/

    val txShiftReg = RegInit(0.U(32.W))
    val rxShiftReg = RegInit(0.U(32.W))
    val bitCounter = RegInit(0.U(8.W))

    val csReg = RegInit(true.B)
    io.spiCs := csReg

    io.spiMosi := txShiftReg(31)
    rxShiftReg := rxShiftReg | Cat(0.U(31.W), io.spiMiso)

    io.leds := Cat(stateReg === active, stateReg === loadData, csReg, bitCounter(3,0))

    val clockEnable = RegInit(false.B)
    //val CNT_MAX = io.prescale - 1.U
    val CNT_MAX = 1.U
    val cntClk = RegInit(0.U(3.W))
    val spiClkReg = RegInit(false.B)

    /*when (clockEnable) {
        when (io.prescale =/= 0.U) {
            cntClk := cntClk + 1.U
            when (cntClk === CNT_MAX) {
                cntClk := 0.U
                spiClkReg := !spiClkReg
            }
        } .otherwise {
            spiClkReg := false.B
        }
    }*/

    when (clockEnable) {
        cntClk := cntClk + 1.U
        when (cntClk === CNT_MAX) {
            cntClk := 0.U
            spiClkReg := !spiClkReg
        }
    }

    io.spiClk := spiClkReg

    val spiClkRegPrev = RegNext(spiClkReg)
    val fallingEdge = !spiClkReg && spiClkRegPrev
    val risingEdge = spiClkReg && !spiClkRegPrev

    val totalCycles = Wire(UInt(8.W))
    totalCycles := io.sendLength + io.receiveLength + io.numWaitCycles

    io.cpuReadData := spiDataOut

    switch (stateReg) {
        is (idle) {
            when (io.enable) {
                stateReg := loadData
            }
        }
        is (loadData) {
            csReg := false.B
            clockEnable := true.B
            txShiftReg := Cat(io.cpuCmd, io.cpuWriteData)
            bitCounter := totalCycles - 1.U
            stateReg := active
        }
        is (active) {

            when (fallingEdge) {
                rxShiftReg := rxShiftReg << 1
                bitCounter := bitCounter - 1.U
                when (bitCounter === 0.U) {
                    stateReg := deassertCS
                } 
            }

            when (risingEdge) {
                txShiftReg := txShiftReg << 1

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