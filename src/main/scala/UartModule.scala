import chisel3._
import chisel3.util._

class UartModule(
                freq: Int,
                baud: Int
                ) extends Module {
  val io = IO(new Bundle{
    val tx = Output(Bool()) //UART tx
    val rx = Input(Bool()) //UART rx
    val rts = Output(Bool()) //UART rts, flow control out
    val cts = Input(Bool()) //UART cts, flow control in
    val tx_data = Input(UInt(8.W)) //Data to transmit to tx from bus
    val tx_valid = Input(Bool())  //low until cpu has data to transmit, then it pulls high
    val tx_ready = Output(Bool()) //default: high, uart controlled, indicates to cpu when available to receive new data
    val rx_data = Output(UInt(8.W)) //Data to store from rx to bus
    val rx_valid = Output(Bool())  //pulses high when valid byte has been captured
    val rx_ready = Input(Bool()) //pulls low when buffer is full and can no longer receive
    val flowControl = Input(Bool()) //determines if flow control is used.
  })

  //internal registers
  val txReady = RegInit(1.U(1.W))
  val txReg = RegInit(0.U(8.W)) //8 bit register for transmitting
  val rxReg = RegInit(0.U(8.W)) //8 bit register for receiving
  val rxDataShift = RegInit(0.U(8.W)) //8 bit register for storing data while still receiving bits
  val txBitCount = RegInit(0.U(3.W)) // Counts bits sent
  val rxBitCount = RegInit(0.U(3.W)) // Counts bits received

  //defaults
  io.tx := true.B //Default to idle (high) state
  io.rx_data := rxReg
  io.rx_valid := RegInit(false.B) // Only pulse when data is actually valid
  io.tx_ready := txReady // Ready to accept new data by default

  //Variables
  val prescaler = (freq / baud)

  //counters for baud rate alignment
  val txCount = RegInit(0.U(32.W)) //counter for transmission
  val rxCount = RegInit(0.U(32.W)) // counter for receiving

  //states for switch case
  val sIdle :: sStartBit :: sDataBits :: sStopBit :: Nil = Enum(4)
  val txState = RegInit(0.U(2.W)) //tx state tracker
  val rxState = RegInit(0.U(2.W)) //rx state tracker

  //TRANSMITTER----------------------------------------------------
  switch(txState){ // idle, start, data and end bit switch statement
    is(sIdle){
      io.tx := 1.B //default high
      txBitCount := 0.U
      when(io.tx_valid && (!io.flowControl || !io.cts)){ // begin transmission when tx_ready is high and check if flow control is needed
        txReg := io.tx_data //loading data from bus to register
        txState := sStartBit //changing state machine
        txCount := 0.U //start counting baud from zero
        txReady := false.B //currently transmitting, no data in allowed
      }
    }
    is(sStartBit){
      //Single start bit, low
      io.tx := false.B
      when(txCount >= prescaler.U - 1.U){ // change states on next baud
        txState := sDataBits
        txCount := 0.U
      }.otherwise{
        txCount := txCount + 1.U //baud counter
      }
    }
    is(sDataBits){
      io.tx := txReg(0)  // always display the bit (LSB first)
      when(txCount >= prescaler.U - 1.U){
        txCount := 0.U
        when(txBitCount === 7.U){
          txState := sStopBit  // Once all bits are stored, move on
        }.otherwise{
          txBitCount := txBitCount + 1.U // increase bit count
          txReg := txReg >> 1 // shift the register once
        }
      }.otherwise{
        txCount := txCount + 1.U //baud counter
      }
    }
    is(sStopBit){
      io.tx := true.B // high stop bit
      when(txCount >= prescaler.U - 1.U){
        txState := sIdle //done transmission, move on
        txCount := 0.U //reset counter
        txReady := true.B //no longer transmitting, ready again
      }.otherwise{
        txCount := txCount + 1.U //baud counter
      }
    }
  }

  //RECEIVER--------------------------------------------------------
  val rxValid = RegNext(false.B) // RegNext keeps this var defaulting to 0, but we will change it later
  io.rx_valid := rxValid
  io.rts := ~io.rx_ready // Will stop transmissions when CPU says buffer is full
  switch(rxState){
    is(sIdle){
      rxBitCount := 0.U //reset count
      rxDataShift := 0.U //reset
      when(io.rx === false.B) { //start to receive message
        rxState := sStartBit
        rxCount := 0.U
      }
    }

    is(sStartBit){
      when(rxCount >= (prescaler.U / 2.U)) { //starting the bit in the middle to give greater leeway to receiving
        when(io.rx === false.B) { //checking start bit is there
          rxState := sDataBits
          rxCount := 0.U
        }.otherwise{
          // no bits, go back to idle
          rxState := sIdle
        }
      }.otherwise{
        rxCount := rxCount + 1.U //baud count
      }
    }

    is(sDataBits){
      when(rxCount >= (prescaler.U - 1.U)){ // changing bits every 1 baud, at a 0.5 baud offset
        rxCount := 0.U
        rxDataShift := Cat(io.rx, rxDataShift(7, 1)) //Takes everything except the zero bit of rxDataShift and adds io.rx to the end (left)

        when(rxBitCount === 7.U){
          rxState := sStopBit //move to stop bit state
        }.otherwise{
          rxBitCount := rxBitCount + 1.U //count up bits to 8
        }
      }.otherwise{
        rxCount := rxCount + 1.U //baud count
      }
    }

    is(sStopBit){
      when(rxCount >= prescaler.U - 1.U){
        when(io.rx === true.B){ //stop bit must be high
          rxReg := rxDataShift // move temp register into final storage location (get put into rxpin every cycle
          rxValid := true.B       // Signal valid data for one cycle for bus to read
        }
        rxState := sIdle // back to idle
        rxCount := 0.U //back to zero
      }.otherwise{
        rxCount := rxCount + 1.U //baud count
      }
    }
  }
}

