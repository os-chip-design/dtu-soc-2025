import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class MemoryInterface extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "Memory Interface"

    it should "Write; acknowledge and set cs" in {
        test(new MemoryInterface16KB(32)) {
          (dut => {
                // Write enable
                dut.io.pipe.wrMask(0).poke(true.B)
                dut.io.pipe.wrMask(1).poke(true.B)
                dut.io.pipe.wrMask(2).poke(true.B)
                dut.io.pipe.wrMask(3).poke(true.B)


                // Don't acknowledge the same clock cycle
                dut.io.pipe.ack.expect(false.B)
                dut.io.memcs.expect(true.B)

                // Acknowledge the following clock cycle
                dut.clock.step(1)
                dut.io.pipe.wrMask(0).poke(false.B)
                dut.io.pipe.wrMask(1).poke(false.B)
                dut.io.pipe.wrMask(2).poke(false.B)
                dut.io.pipe.wrMask(3).poke(false.B)

                dut.io.pipe.ack.expect(true.B)
                dut.io.memcs.expect(false.B)

                // Set acknowledge low after no transaction
                dut.clock.step(1)
                dut.io.pipe.ack.expect(false.B)
            })
        }
    }

    it should "Read; acknowledge and set cs" in {
        test(new MemoryInterface16KB(32)) {
            dut => {
                // Write enable
                dut.io.pipe.rd.poke(true.B)

                // Don't acknowledge the same clock cycle
                dut.io.pipe.ack.expect(false.B)

                // Set cs high
                dut.io.memcs.expect(true.B)

                // Acknowledge the following clock cycle
                // and set memcs low
                dut.clock.step(1)
                dut.io.pipe.rd.poke(false.B)
                dut.io.pipe.ack.expect(true.B)
                dut.io.memcs.expect(false.B)

                // Set acknowledge low after no transaction
                dut.clock.step(1)
                dut.io.pipe.ack.expect(false.B)
            }
        }
    }

    it should "Slice the first 12 bits of the address" in {
        test(new MemoryInterface16KB(32)) {
            dut => {
                // Input 32bit
                dut.io.pipe.address.poke("hDEADBEEF".U(32.W))

                dut.io.memaddress.expect("hEEF".U(12.W))
            }
        }
    }
}
