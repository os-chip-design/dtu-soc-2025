## Off chip memory notes 

# Pmod
Current plan is to use the [TinyTapeout QSPI Pmod](https://github.com/mole99/qspi-pmod) as the off-chip memory.

The QSPI Pmod contains one SPI flash memory and two [p-SRAMS](https://en.wikipedia.org/wiki/Dynamic_random-access_memory#PSRAM) memories.

A flash memory is non-volatile, meaning that it retains its data even when the power is turned off. The p-SRAMs are volatile.
 
The Pmod has a total of 8 pins, with three of them being chip select pins (PMOD1, PMOD7 & PMOD8) used to choose which memory to communicate with. 
The clock pin is PMOD4. 
The data pins are PMOD2, PMOD3, PMOD5 and PMOD6. For non-quad SPI, MOSI is PMOD2 & MISO is PMOD3.

Through inspecting the [KiCad schematic](https://github.com/mole99/qspi-pmod/releases/download/v2.1/qspi-pmod.pdf) in the releases section of the repository, the SPI flash memory is given as a W25Q128JV-SIQ and the p-SRAMs are given as 878-APS6404L-3SQR-SN.

Googling these part numbers has led to the following data sheets:
- [W25Q128JV](https://docs.rs-online.com/7d70/0900766b81703faf.pdf).
- [APS6404L-3SQR](https://github.com/Edragon/Datasheet/blob/master/APM/APS6404L-3SQR-SN-2.pdf).

According to the data sheets, the W25Q128JV has a capacity of 128Mbits and the APS6404L-3SQR has a capacity of 64Mbits.

That means there is 128Mb of non-volatile memory and 128Mb of volatile memory on the Pmod.

Instructions for interacting with the memories are in the datasheets and have to be explored. 
The instructions will be transfered using SPI.

# SD-Card
The SD-Card is another option for off-chip memory.
[The wiki](https://en.wikipedia.org/wiki/SD_card#Technical_details) says that SD-cards use SPI for communication.
Note it does not mention quad SPI support.


# Notes from 12/03/2025
// We want to convert from write or read transaction? to (Q)-SPI signals to communicate with off-chip memory pmod.

// PMOD1 -> Chip Select 1 (Flash)
// PMOD2 -> MOSI / Data 0 / SIO0
// PMOD3 -> MISO / Data 1 / SIO1
// PMOD4 -> Clock
// PMOD5 -> Data 2 / SIO2
// PMOD6 -> Data 3 / SIO3 
// PMOD7 -> Chip Select 2 (RAM A)
// PMOD8 -> Chip Select 3 (RAM B)


// The Pmod has a total of 8 pins, with three of them being chip select pins (PMOD1, PMOD7 & PMOD8) 
// used to choose which memory to communicate with. The clock pin is PMOD4. 
// The data pins are PMOD2, PMOD3, PMOD5 and PMOD6. For non-quad SPI, MOSI is PMOD2 & MISO is PMOD3.


// Questions for Luca:
// * What is the input
//    -  Use a generic interface for now
//    - (AXI) Group behind us figure something out (group 20)
// * Do we want/have to support all of SPI/QSPI/FastSPI?
//    - Not really, we just need to be able o talk with them
// * SD-Card?
//    - Do it later, should interface with SPI. Find some SD-card
// * Same input and output?
//     - in, out, outEnable, 3 state buffer verilog
// * How does Quad version work if CPU only does one at a time?
//     - Only use quad where it makes sense
// * What about the delaying the clock?
//    - one clock, use counters for delaying the clock so we are in sync with the memory

// * Verification plan?
//    - Agree with everyone and beat people with sticks 
//    - Come up with some general requirements, i.e. everyone use their specific address space in system level test
//    - Discuss CI with Martin
