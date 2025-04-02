/*
 * Simple SPI Controller for 02118
 * 
 * Author: Group4
 *
*/

package SpiControllerG4

import chisel3._
import chisel3.util._


//Just a comment for Git test 
class SpiControllerG4 extends Module {
    val io = IO(new Bundle {
        val cpuWriteData = Input(UInt(32.W))
        val cpuReadData = Output(UInt(32.W))
        val enable = Input(Bool())
        val sendLength = Input(UInt(8.W))
        val receiveLength = Input(UInt(8.W))
        val rw = Input(Bool()) // 0 when also reciving data, 1 for sending only

        val spiMiso = Input(Bool())
        val spiMosi = Output(Bool())
        val spiCs = Output(Bool())
        val spiClk = Output(Bool())

        val done = Output(Bool())
        
        val prescale = Input(UInt(4.W))
    })

    object State extends ChiselEnum {
        val idle, loadData, assertCS, sendData, receiveData, deassertCS = Value
    }
    import State._
    val stateReg = RegInit(idle)

    val CNT_MAX = io.prescale - 1.U
    val cntClk = RegInit(0.U(3.W))
    val spiClkReg = RegInit(false.B)

    val spiData = RegInit(0.U(32.W))
    val txBuffer = RegInit(0.U(32.W))
    val txBufferEmpty = RegInit(true.B)
    val txShiftReg = RegInit(0.U(32.W))
    val txShiftRegEmpty = RegInit(true.B)

    val spiBuffer = RegInit(0.U(32.W))
    val rxBuffer = RegInit(0.U(32.W))
    val rxBufferEmpty = RegInit(true.B)
    val rxShiftReg = RegInit(0.U(32.W))

    val bitCounter = RegInit(0.U(8.W))

    val csReg = RegInit(true.B)
    io.spiCs := csReg

    val doneReg = RegInit(false.B)
    io.done := doneReg

    io.cpuReadData := 0.U
    io.spiMosi := txShiftReg(31)
    // rxShiftReg(31) := io.spiMiso , cannot assign a register combinationally
    spiData := io.cpuWriteData

    when (txBufferEmpty) {
        txBuffer := spiData
        txBufferEmpty := false.B
    }

    when (!rxBufferEmpty) {
        spiBuffer := rxBuffer
        rxBufferEmpty := true.B
    }

    cntClk := cntClk + 1.U
    when (cntClk === CNT_MAX) {
        cntClk := 0.U
        spiClkReg := !spiClkReg
    }
    io.spiClk := spiClkReg

    switch (stateReg) {
        is (idle) {
            doneReg := false.B
            when (io.enable) {
                stateReg := loadData
            }
        }
        is (loadData) {
            txShiftReg := txBuffer
            bitCounter := io.sendLength
            stateReg := assertCS
        }
        is (assertCS) {
            csReg := false.B
            stateReg := sendData
        }
        is (sendData) {
            when (spiClkReg) {
                txShiftReg := txShiftReg << 1
                bitCounter := bitCounter - 1.U
                when (bitCounter === 0.U) {
                    txShiftRegEmpty := true.B

                    when (!io.rw) {
                        stateReg := receiveData
                    } .otherwise {
                        stateReg := deassertCS
                    }
                }
            }
        }
        is (receiveData) {
            when (spiClkReg) {
                rxShiftReg := rxShiftReg << 1
                bitCounter := bitCounter + 1.U
                when (bitCounter === io.receiveLength) {
                    when (rxBufferEmpty) {
                        rxBuffer := rxShiftReg
                        txBufferEmpty := false.B
                        stateReg := deassertCS
                    }
                }
            }
        }
        is (deassertCS) {
            //CS := 1.U
            //stateReg := idle
            csReg := true.B
            doneReg := true.B
            stateReg := idle
        }
    }

}

object SpiController extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SpiControllerG4())
}
