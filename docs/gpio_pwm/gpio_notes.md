# GPIO notes

## Digital part


## Analog part 
Tinytapeout uses [sky130_ef_io_gpiov2_pad](https://skywater-pdk.readthedocs.io/en/main/contents/libraries/sky130_fd_io/docs/user_guide.html#sky130-fd-io-gpiov2-additional-features) macro for their output.
- Input/output on the same pad.
- Has pull up and pull down.
- Open-drain and different drive strength.
- ESD protection (series resistor and clamping diodes).
- Fmax of 33 MHz.
- Voltage range 1.71V to 5.5V
- Includes input buffer

This is a good candidate and also allows using TT's design as reference, such as: [TT10](https://github.com/TinyTapeout/tinytapeout-10) or specifcally [TT-Mux](https://github.com/TinyTapeout/tt-multiplexer/)

Thus using the skyworks macro, and possibly using a wrapper for simplifying solves the task of the analog front end.

General IO information of [sky130_fd_io](https://skywater-pdk.readthedocs.io/en/main/contents/libraries/sky130_fd_io/docs/user_guide.html)

Skywater PDK location of [gpiov2](https://github.com/google/skywater-pdk-libs-sky130_fd_io/tree/01b18699b4102d8e54ad1406b3991ecb652e5aee/cells/top_gpiov2)