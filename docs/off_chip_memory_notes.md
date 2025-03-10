# Off chip memory notes 

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