/*
 * Simple SPI Controller for 02118
 * 
 * Author: Group4
 *
*/

import chisel3._
import chisel3.util._

class SpiControllerG4 extends Module {
    val io = IO(new Bundle {
        val cpuWriteData = Input(UInt(32.W))
        val cpuReadData = Output(UInt(32.W))
        val enable = Input(Bool())

        val spiMiso = Input(Bool())
        val spiMosi = Output(Bool())
        val spiCs = Input(Bool())
        val spiClk = Output(Bool())
        
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
    val txShiftReg = RegInit(0.U(8.W))

    val spiBuffer = RegInit(0.U(32.W))
    val rxBuffer = RegInit(0.U(32.W))
    val rxShiftReg = RegInit(0.U(8.W))

    io.cpuReadData := 0.U
    io.spiMosi := false.B

    cntClk := cntClk + 1.U
    when (cntClk === CNT_MAX) {
        cntClk := 0.U
        spiClkReg := !spiClkReg
    }
    io.spiClk := spiClkReg

    /*switch (stateReg) {
        is (idle) {
            when (enable) {
                stateReg := loadData
            }
        }
        is (loadData) {

        }
        is (assertCS) {

        }
        is (sendData) {

        }
        is (receiveData) {

        }
        is (deassertCS) {
            //CS := 1.U
            //stateReg := idle
        }
    }*/

}

object SpiController extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SpiControllerG4())
}
