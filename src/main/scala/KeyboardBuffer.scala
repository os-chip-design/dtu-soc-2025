import chisel3._
import chisel3.util._


class Buffer extends Module {
    val io = IO(new Bundle {
        // Input interface
        val dataIn = Input(UInt(8.W))       // Data from the keyboard to be saved
        val ready = Output(Bool())          // Ready to receive new data from keyboard
        val valid = Input(Bool())           // Keyboard controller indicating valid data available
        
        // Output Interface
        val validOut = Output(Bool())       // Indicating valid data for bus
        val dataOut = Output(UInt(32.W))     // Data to be sent from buffer
        val request = Input(Bool())         // Request flag from bus
    })

    // Initialization
    def rising(v: Bool) = v & !RegNext(v)
    def falling(v: Bool) = !v & RegNext(v)

    val buffer = SyncReadMem(256, UInt(8.W))
    
    io.dataOut := 0.U
    io.ready := 0.U
    io.validOut := 0.U

    // Registers
    val readCnt = RegInit(0.U(8.W))
    val writeCnt = RegInit(0.U(8.W))
    val verifyCatch = RegInit(0.U(8.W))
    val dataIn = RegInit(0.U(8.W))
    val dataOut = RegInit(0.U(8.W))
    val new_data = RegInit(0.B)
    val new_read = RegInit(0.B)

    // *** Datapath ***
    when (rising(io.valid)){new_data := 1.B}
    when (rising(io.request)){new_read := 1.B}

    // *** FSM ***
    object State extends ChiselEnum {
        // val IDLE, WRITE, VERIFYREAD, VERIFY, OUTPUTREAD, OUTPUT = Value
        val IDLE, WRITE, OUTPUT, NODATA = Value
    }

    import State._
    val stateReg = RegInit(IDLE)

    switch(stateReg){
        is (IDLE) {
            readCnt := readCnt
            writeCnt := writeCnt
            // dataIn := io.dataIn

            // I/O
            io.dataOut := 0.U
            io.ready := 1.B
            io.validOut := 0.U
            dataOut := buffer.read(readCnt)

            when (new_data) {stateReg := WRITE}
            .elsewhen (new_read && readCnt =/= writeCnt) {stateReg := OUTPUT}
            .elsewhen (new_read && readCnt === writeCnt) {stateReg := NODATA}
        }

        is (WRITE) {
            io.ready := 0.B
            buffer.write(writeCnt, io.dataIn)
            writeCnt := writeCnt + 1.U
            new_data := 0.B
            stateReg := IDLE
        }

        is (OUTPUT) {
            io.ready := 1.B
            io.dataOut := Cat("b000000000000000000000000".U,dataOut) //Update data output
            io.validOut := 1.B
            new_read := 0.B

            
            when (!io.request) {
                readCnt := readCnt + 1.U
                stateReg := IDLE
            }
        }
        is (NODATA) {
            io.ready := 1.B
            io.dataOut := 0.U
            dataOut := 0.U
            io.validOut := 1.B
            new_read := 0.B

            when (!io.request) {
                stateReg := IDLE
            }
        }
    }
    when(!(stateReg === IDLE || stateReg === WRITE || stateReg === OUTPUT || stateReg === NODATA)) {stateReg := IDLE}
}