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

### VgaTop Pin Summary

This table summarizes the primary inputs and outputs of the `VgaTop` module.

| Signal Name          | Direction | Type / Width  | Clock Domain | Description                                                                 |
| :------------------- | :-------- | :------------ | :----------- | :-------------------------------------------------------------------------- |
| **Write Interface**  |           |               | **System**   | Write port to Character Buffer (via FIFO - Decoupled protocol).             |
| `io.write.valid`     | Input     | `Bool [0]`    | System       | Asserts high when `addr` and `data` are valid.                              |
| `io.write.bits.addr` | Input     | `UInt [11:0]` | System       | Address (0-2399) for Character Buffer write. Refer to table in next section |
| `io.write.bits.data` | Input     | `UInt [15:0]` | System       | Data (Attribute[15:8] + Char Code[7:0]) for Char Buffer write.              |
| `io.write.ready`     | Output    | `Bool [0]`    | System       | Asserts high when FIFO is ready to accept write transaction.                |
|                      |           |               |              |                                                                             |
| **VGA Outputs**      |           |               | **VGA**      | Generated VGA signals (25MHz domain).                                       |
| `io.vgaClkOut`       | Output    | `Clock`       | VGA          | Generated 25MHz pixel clock output.                                         |
| `io.hSync`           | Output    | `Bool [0]`    | VGA          | Horizontal Sync pulse (active low).                                         |
| `io.vSync`           | Output    | `Bool [0]`    | VGA          | Vertical Sync pulse (active low).                                           |
| `io.r`               | Output    | `UInt [3:0]`  | VGA          | 4-bit Red color data.                                                       |
| `io.g`               | Output    | `UInt [3:0]`  | VGA          | 4-bit Green color data.                                                     |
| `io.b`               | Output    | `UInt [3:0]`  | VGA          | 4-bit Blue color data.                                                      |

### VgaCharacterBuffer Data Format (16 bits)

Each location in the `VgaCharacterBuffer` stores a 16-bit value that defines the character to be displayed and its visual attributes (foreground color, background color, blink). The structure is as follows:

| Bit Range | Size (bits) | Name                    | Description                                                                                                                        |
| :-------- | :---------- | :---------------------- | :--------------------------------------------------------------------------------------------------------------------------------- |
| `[15]`    | 1           | Blink (B)               | Enables character blinking (often ignored or handled by specific display hardware; not implemented in the provided PixelRenderer). |
| `[14:12]` | 3           | Background Color (BGR)  | 3-bit index (0-7) specifying the background color from the lower 8 entries of the standard 16-color palette.                       |
| `[11:8]`  | 4           | Foreground Color (IRGB) | 4-bit index (0-15) specifying the foreground color from the standard 16-color palette.                                             |
| `[7:0]`   | 8           | Character Code          | The 8-bit ASCII code (0-255) for the character glyph to be looked up in the `VgaFontRom`.                                          |

**Note:** The `VgaPixelRenderer` provided uses bits `[11:8]` (`attributeData[3:0]`) for the foreground index and bits `[14:12]` (`attributeData[6:4]`) for the background index, directly mapping to the standard 16-color VGA text mode attribute byte format. Bit `[15]` (`attributeData[7]`) is not used but is normally blinking.
