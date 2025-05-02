import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class GPIOShiftRegisterTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "GPIOShiftRegister test"

    it should "pass" in {
        test(
            new GPIOShiftRegister(
                    bit_width = 32
                )
            ).withAnnotations(Seq( WriteVcdAnnotation )) {
            dut => {

                //println("")
                //println("Clocking out data")
                
                for (i <- 0 until 32) {
                    dut.clock.step()
                    dut.io.rd.poke(true.B)
                    //print(s"$i: ${dut.io.read_data.peek().litToBoolean} ")
                    dut.io.read_data.expect(false.B)
                    
                    // Toggle write data to ensure nothing gets written
                    if (((i+1) % 2) == 0) {
                        dut.io.write_data.poke(false.B)
                    } else {
                        dut.io.write_data.poke(true.B)
                    }

                    // Also check the conf output
                    for (i <- 0 until 31) {
                        dut.io.conf_output(i).expect(false.B)
                    }


                }
                dut.io.rd.poke(false.B)
                dut.io.write_data.poke(false.B)

                // println("")
                // println("Clocking in data")
                for (i <- 0 until 32) {
                    dut.clock.step()
                    //print(s"$i: ")
                    dut.io.wr.poke(true.B)

                    if (((i+1) % 2) == 0) {
                        dut.io.write_data.poke(false.B)
                        //print("0 ")
                    } else {
                        dut.io.write_data.poke(true.B)
                        //print("1 ")
                    }
                    
                    for (i <- 0 until 32) {
                    dut.io.conf_output(i).expect(false.B)
                    }
                }
                dut.io.wr.poke(false.B)
                dut.io.write_data.poke(false.B)
                dut.clock.step(2)
                
                // Check the conf output now
                // println("")
                // println("Checking output")
                for (i <- 0 until 32) {
                    //print(s"$i: ${dut.io.conf_output(i).peek().litToBoolean} ")
                    if ((i % 2) == 0) {
                        dut.io.conf_output(i).expect(true.B)
                    } else {
                        dut.io.conf_output(i).expect(false.B)
                    }
                }

                // Now try to clock out some data again
                // First try to clock out some data.
                // println("")
                // println("Clocking out data again")
                dut.io.rd.poke(true.B)
                for (i <- 0 until 32) {
                    dut.clock.step()
                    
                    if (((i+1) % 2) == 0) {
                        dut.io.read_data.expect(false.B)
                    } else {
                        dut.io.read_data.expect(true.B)
                    }

                    if (((i+1) % 2) == 0) {
                        dut.io.write_data.poke(false.B)
                    } else {
                        dut.io.write_data.poke(true.B)
                    }

                    for (i <- 0 until 32) {
                        //print(s"$i: ${dut.io.conf_output(i).peek().litToBoolean} ")
                        if ((i % 2) == 0) {
                            dut.io.conf_output(i).expect(true.B)
                        } else {
                            dut.io.conf_output(i).expect(false.B)
                        }
                    }
                }
                dut.io.rd.poke(false.B)
                dut.io.write_data.poke(false.B)

                for (i <- 0 until 20) {
                    dut.clock.step()
                }
            }
        }
    }
}