import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Memory2Pipecon extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "Native Memory to Pipecon interface"

    it should "Write; acknowledge and set cs" in {
        test(new NativeMemory2Pipecon()) {
          (dut => {
                // Write enable
                dut.io.pipe.wr.poke(true.B)


                // Don't acknowledge the same clock cycle
                dut.io.pipe.ack.expect(false.B)
                dut.io.native.cs.expect(false.B)

                // Acknowledge the following clock cycle
                dut.clock.step(1)
                dut.io.pipe.wr.poke(false.B)

                dut.io.pipe.ack.expect(true.B)
                dut.io.native.cs.expect(true.B)

                // Set acknowledge low after no transaction
                dut.clock.step(1)
                dut.io.pipe.ack.expect(false.B)
            })
        }
    }

    it should "Read; acknowledge and set cs" in {
        test(new NativeMemory2Pipecon()) {
            dut => {
                // Write enable
                dut.io.pipe.rd.poke(true.B)

                // Don't acknowledge the same clock cycle
                dut.io.pipe.ack.expect(false.B)

                // Set cs low (active low)
                dut.io.native.cs.expect(false.B)

                // Acknowledge the following clock cycle
                // and set memcs high
                dut.clock.step(1)
                dut.io.pipe.rd.poke(false.B)
                dut.io.pipe.ack.expect(true.B)
                dut.io.native.cs.expect(true.B)

                // Set acknowledge low after no transaction
                dut.clock.step(1)
                dut.io.pipe.ack.expect(false.B)
            }
        }
    }

    it should "Slice the first 9 bits of the address" in {
        test(new NativeMemory2Pipecon(ADDR_WIDTH = 9)) {
            dut => {
                // Input 32bit
                dut.io.pipe.address.poke("hDEADBEEF".U(32.W))

                dut.io.native.address.expect("b011101111".U(9.W))
            }
        }
    }
}
