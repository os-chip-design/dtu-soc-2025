# VGA Controller Module Descriptions

This file provides a brief description of each module within the VGA text-mode controller design.

## VgaConfig (`VgaConfig.scala`)

Config for the VGA controller (Generally don't change anything in here)

## VgaTimer (`VgaTimer.scala`)

Acts as the master timing generator for the VGA signal. Based on a pixel clock input (assumed 25.175 MHz), it uses internal counters to generate the horizontal (HSync) and vertical (VSync) synchronization pulses according to VESA standards. It also outputs signals indicating when the electron beam is within the active display area (`hActive`, `vActive`) and the current pixel coordinates (`pixelX`, `pixelY`).

## VgaCharacterIndexer (`VgaCharacterIndexer.scala`)

Translates the raw pixel coordinates (`pixelX`, `pixelY`) from the `VgaTimer` into character-based coordinates. It calculates which character row and column corresponds to the current pixel, and also the specific X and Y offset _within_ that character's glyph (e.g., pixel 3 of row 5 within an 8x16 character). It also calculates the address required to fetch the _next_ character's data from the `VgaCharacterBuffer`, compensating for its read latency.

## VgaCharacterBuffer (`VgaCharacterBuffer.scala`)

This module implements the screen's character memory using a synchronous RAM. It stores a 16-bit value for each character cell on the screen (typically 8 bits for the character code and 8 bits for attributes like color). It features distinct read and write ports. Writes occur on the clock edge when write enable is high. Reads have a one-cycle latency: the data for a requested address appears on the output the cycle _after_ the address is presented.

## VgaFontRom (`VgaFontRom.scala`)

A Read-Only Memory (ROM) that stores the bitmap data (glyphs) for every character in the font set (e.g., ASCII characters 0-255). It takes a character code and a vertical row index within the glyph as input and outputs the corresponding row of pixel data (typically 8 bits for an 8-pixel wide font). The font data itself is loaded from a binary file during the hardware synthesis process.

## VgaPixelRenderer (`VgaPixelRenderer.scala`)

This is the final stage responsible for generating the actual RGB color output for each pixel. It receives the attribute byte (containing foreground/background color information) from the `VgaCharacterBuffer` and the relevant row of font pixel data from the `VgaFontRom`. Using the pixel's horizontal index within the character glyph, it determines if the pixel should display the foreground or background color. It then looks up the appropriate 4-bit R, G, and B values from an internal 16-color VGA palette and outputs them, but only when the `VgaTimer` indicates the pixel is within the active display area.

## VgaController (`VgaController.scala`)

This module acts as the central hub connecting all the other VGA sub-modules (For a synchronous system). It routes the timing signals from the `VgaTimer` to the `VgaCharacterIndexer`, connects the indexer's outputs to the address ports of the `VgaCharacterBuffer` and `VgaFontRom`, feeds the data from these memories (respecting latency) to the `VgaPixelRenderer`, and passes the final RGB and Sync signals to its own outputs. It also handles the external write interface, passing write requests directly to the `VgaCharacterBuffer`. Do not use in chip unless it runs at 25MHZ

## VgaTop (`VgaTop.scala`)

This is the top level module, and wraps the VgaController with a clock divider to go form 100mhz to 25 mhz, as well as an async FIFO to handle writing to the character buffer from a different clock domain
