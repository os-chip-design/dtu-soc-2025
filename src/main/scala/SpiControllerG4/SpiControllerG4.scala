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
        val cpuCommand = Input(UInt(8.W))
        val cpuWriteData = Input(UInt(32.W))
        val cpuReadData = Output(UInt(32.W))
        val enable = Input(Bool())
        val sendLength = Input(UInt(6.W))
        val rw = Input(Bool()) // 0 when also reciving data, 1 for sending only

        val spiMiso = Input(Bool())
        val spiMosi = Output(Bool())
        val spiCs = Output(Bool())
        val spiClk = Output(Bool())

        val ready = Output(Bool())
        
        val prescale = Input(UInt(4.W))
    })

    object State extends ChiselEnum {
        val idle, loadData, sendData, receiveData, deassertCS = Value
    }
    import State._
    val stateReg = RegInit(idle)

    val CNT_MAX = io.prescale - 1.U
    val cntClk = RegInit(0.U(3.W))
    val spiClkReg = RegInit(false.B)

    val spiData = RegInit(0.U(32.W))
    //val txBuffer = RegInit(0.U(32.W))
    //val txBufferEmpty = RegInit(true.B)
    val txShiftReg = RegInit(0.U(40.W))
    //val txShiftRegEmpty = RegInit(true.B)

    //val spiBuffer = RegInit(0.U(32.W))
    //val rxBuffer = RegInit(0.U(32.W))
    //val rxBufferEmpty = RegInit(true.B)
    val rxShiftReg = RegInit(0.U(32.W))

    val bitCounter = RegInit(0.U(8.W))

    val csReg = RegInit(true.B)
    io.spiCs := csReg

    io.cpuReadData := 0.U
    io.spiMosi := txShiftReg(39)
    rxShiftReg := rxShiftReg | Cat("b0000000000000000000000000000000".U, io.spiMiso)

    /*when (txBufferEmpty) {
        txBuffer := spiData
        txBufferEmpty := false.B
    }

    when (!rxBufferEmpty) {
        spiBuffer := rxBuffer
        rxBufferEmpty := true.B
    }*/

    when (io.prescale =/= 1.U) {
        cntClk := cntClk + 1.U
        when (cntClk === CNT_MAX) {
            cntClk := 0.U
            spiClkReg := !spiClkReg
        }
    } .otherwise {
        spiClkReg := !spiClkReg
    }
    io.spiClk := spiClkReg

    // Edge detection registers
    val spiClkRegPrev = RegNext(spiClkReg)
    val fallingEdge = !spiClkReg && spiClkRegPrev
    val risingEdge = spiClkReg && !spiClkRegPrev

    switch (stateReg) {
        is (idle) {
            when (io.enable) {
                stateReg := loadData
            }
        }
        is (loadData) {
            csReg := false.B
            txShiftReg := Cat(io.cpuWriteData, 0.U(8.W))
            bitCounter := io.sendLength
            stateReg := sendData
        }
        is (sendData) {
            when (fallingEdge) {
                txShiftReg := txShiftReg << 1
                bitCounter := bitCounter - 1.U

                when (bitCounter === 0.U) {
                    stateReg := Mux(!io.rw, receiveData, deassertCS)
                }
            }
        }
        is (receiveData) {
            when(risingEdge){
                rxShiftReg := rxShiftReg << 1
                bitCounter := bitCounter + 1.U
                when(bitCounter === 32.U) {
                    io.cpuReadData := rxShiftReg
                    stateReg := deassertCS
                }
            }
        }
        is (deassertCS) {
            csReg := true.B
            stateReg := idle
        }
    }

    when (stateReg === idle) {
        io.ready := true.B
    } .otherwise {
        io.ready := false.B
    }
}

object SpiController extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SpiControllerG4())
}
