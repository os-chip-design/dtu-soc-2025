import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random
import scala.collection.mutable

class VgaCharacterBufferTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "CharacterBuffer (16-bit)"

  val characterHeight = 16
  val characterWidth = 8
  val cols = 640 / characterWidth
  val rows = 480 / characterHeight
  val bufferSize = cols * rows // e.g., 2400
  val addrWidth = 12

  it should "write and read back random 16-bit data at random addresses" in {
    test(new CharacterBuffer()).withAnnotations(Seq(WriteVcdAnnotation)) {
      dut =>
        val rng = new Random(0) // Use fixed seed for reproducibility
        val numWrites = 20 // Number of random writes to perform
        val referenceMem =
          mutable.Map[BigInt, BigInt]() // Software model of the memory

        // --- Phase 1: Random Writes ---
        dut.io.write.enable.poke(true.B) // Keep write enabled during this phase
        for (_ <- 0 until numWrites) {
          // Generate random address and data
          val randomAddr =
            BigInt(rng.nextInt(bufferSize)) // Address within buffer bounds
          val randomData = BigInt(16, rng) // Random 16-bit value

          // Poke write port
          dut.io.write.addr.poke(randomAddr.U)
          dut.io.write.data.poke(randomData.U(16.W))

          // Update reference model
          referenceMem(randomAddr) = randomData

          // Step clock for write to occur
          dut.clock.step()
        }
        dut.io.write.enable.poke(false.B) // Disable writes for the read phase

        // --- Phase 2: Random Reads and Verification ---
        // Get the list of addresses that were actually written
        val writtenAddresses =
          rng.shuffle(referenceMem.keys.toList) // Shuffle for random read order

        var previousReadAddr: Option[BigInt] =
          None // To track address from previous cycle

        // Iterate through the addresses we wrote to
        for (currentReadAddr <- writtenAddresses) {
          // Set the read address for the *current* cycle
          dut.io.read.addr.poke(currentReadAddr.U)

          // Check the data output from the *previous* cycle's read address
          previousReadAddr match {
            case Some(addr) =>
              val expectedData = referenceMem(addr)
              dut.io.read.data.expect(
                expectedData.U(16.W),
                f"Read Addr $addr: Expected $expectedData%04X, Got ${dut.io.read.data.peek().litValue}%04X"
              )
            case None => // First read cycle, no previous data to check
          }

          // Store the current address for the next cycle's check
          previousReadAddr = Some(currentReadAddr)

          dut.clock.step()
        }

        // After the loop, perform one last check for the data corresponding
        // to the last address poked into dut.io.read.addr
        previousReadAddr match {
          case Some(addr) =>
            val expectedData = referenceMem(addr)
            dut.io.read.data.expect(
              expectedData.U(16.W),
              f"Final Read Addr $addr: Expected $expectedData%04X, Got ${dut.io.read.data.peek().litValue}%04X"
            )
          case None =>
            assert(
              writtenAddresses.isEmpty,
              "Test logic error: No addresses were read wtf???"
            )
        }
    }
  }
}
