import chisel3._
import chisel3.util._
// Utility classes for SPI and QSPI interfaces
// and the instructions from W25Q32JV datasheet 
// (https://docs.rs-online.com/7d70/0900766b81703faf.pdf)

class spiIO extends Bundle {
  val spiClk = Output(Bool())
  val chipSelect = Output(Bool())

  val dataIn = Input(Bool())
  val dataOut = Output(Bool())
}

class qspiIO extends Bundle {
  val spiClk = Output(Bool())
  val chipSelect = Output(Bool())

  val data0In = Input(Bool())
  val data1In = Input(Bool())
  val data2In = Input(Bool())
  val data3In = Input(Bool())
  val data0Out = Output(Bool())
  val data1Out = Output(Bool())
  val data2Out = Output(Bool())
  val data3Out = Output(Bool())
}

object SPIInstructions {
  val readJEDECInstruction   =  "b10011111".U // 0x9F (Read JeDEC ID), table 8.1.3
  val writeEnableInstruction =  "b00000110".U // 0x06 (Write Enable), table 8.13
  val pageProgramInstruction =  "b00000010".U // 0x02 (Page Program), table 8.1.3
  val readDataInstruction    =  "b00000011".U // 0x03 (Read Data), table 8.1.3

  val chipEraseInstruction  =  "b11000111".U // 0xC7 (Chip Erase), table 8.1.3
  val readStatusRegister1Instruction = "b00000101".U // 0x05 (Read Status Register 1), table 8.1.3
}