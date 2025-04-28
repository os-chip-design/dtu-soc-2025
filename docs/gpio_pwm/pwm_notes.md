# PWM and Timer notes

## Timer
A counter will be used to generate timers used for the following modules. The counter is derived from the system clock, which is estimated to be approx 100 MHz.
This will require a prescaler allowing to divide the clock down to lower frequencies.

## Interrupt
The current state of [wildcat](https://github.com/schoeberl/wildcat) does not support interrupts.

## PWM
The PWM should have configurable frequency and pulse width.
