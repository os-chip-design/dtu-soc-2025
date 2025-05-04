import chisel3._
import chisel3.util._
import VgaConfig._

/** Bundle for the Write Port of the Character Buffer. Stores a 16-bit word
  * (Attribute + Character Code).
  */
class CharacterBufferWritePort extends Bundle {
  // Use the address width defined in VgaConfig
  val addr = Input(UInt(CHAR_BUFFER_ADDR_WIDTH.W)) // Address to write to
  val data = Input(UInt(16.W)) // Standard 16-bit for char+attribute
  val enable = Input(Bool()) // Write enable signal (active high)
}

/** Bundle for the Read Port of the Character Buffer. Reads a 16-bit word
  * (Attribute + Character Code). Uses address width from VgaConfig.
  */
class CharacterBufferReadPort extends Bundle {
  // Use the address width defined in VgaConfig
  val addr = Input(UInt(CHAR_BUFFER_ADDR_WIDTH.W)) // Address to read from
  val data = Output(UInt(16.W)) // Standard 16-bit for char+attribute
}

/** Character Buffer RAM using SyncReadMem (16-bit data width). Stores the
  * Attribute+Character codes for the text display. Size and address width are
  * determined by constants in VgaConfig.
  */
class CharacterBuffer extends Module {

  val bufferSize = CHAR_COLS * CHAR_ROWS // Total number of character locations

  val io = IO(new Bundle {
    val write = new CharacterBufferWritePort()
    val read = new CharacterBufferReadPort()
  })

  // Create the synchronous memory with size from VgaConfig
  val memory = SyncReadMem(bufferSize, UInt(16.W))

  // --- Write Logic ---
  when(io.write.enable) {
    memory.write(io.write.addr, io.write.data)
  }

  // --- Read Logic ---
  io.read.data := memory.read(io.read.addr, true.B)
}
