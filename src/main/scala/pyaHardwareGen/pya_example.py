# Example script that creates an input, an output, and a power/ground pin.
# No logic, just pads.
import pya

layout = pya.Layout()

top = layout.create_cell("t")

# Layers
m1 = layout.layer(68, 20) # metal1
m1label = layout.layer(68, 5) # metal1 label
m2 = layout.layer(69, 20) # metal2
m2pin = layout.layer(69, 16) # metal2 pin
m2label = layout.layer(69, 5) # metal2 label
via2 = layout.layer(69, 44) # via2
m3 = layout.layer(70, 20) # metal3
via3 = layout.layer(70, 44) # via3
m4 = layout.layer(71, 20) # metal4
m4pin = layout.layer(71, 16) # metal4 pin
m4label = layout.layer(71, 5) # metal4 label
via1 = layout.layer(68, 44) # via1
m1pin = layout.layer(68,16)
bd = layout.layer(235, 4) # boundary
mc = layout.layer(81, 2) # memcore
li1 = layout.layer(67, 20)
li1pin = layout.layer(67, 16)
mcon1 = layout.layer(67, 44)





# Scaling parameters, experimental
scale_factor = 100
via_scale_factor = 1



# Defines the bounding box and sets everything as memcore
top.shapes(bd).insert(pya.Box(0 * scale_factor, 0 * scale_factor, 10000 * scale_factor, 5000 * scale_factor))
top.shapes(mc).insert(pya.Box(0 * scale_factor, 0 * scale_factor, 10000 * scale_factor, 5000 * scale_factor))


# Update via dimensions - does not adhere to DRC rules as of now, should be adjusted.
via_width = int(150 * via_scale_factor)
via_height = int(150 * via_scale_factor)

# Via definitions
via1_box_1 = pya.Box(100 * scale_factor, 100 * scale_factor, (100 * scale_factor) + via_width, (100 * scale_factor) + via_height)
via1_box_2 = pya.Box(8100 * scale_factor, 100 * scale_factor, (8100 * scale_factor) + via_width, (100 * scale_factor) + via_height)
via1_box_3 = pya.Box(8100 * scale_factor, 3100 * scale_factor, (8100 * scale_factor) + via_width, (3100 * scale_factor) + via_height)
via1_box_4 = pya.Box(100 * scale_factor, 3100 * scale_factor, (100 * scale_factor) + via_width, (3100 * scale_factor) + via_height)

via2_box_1 = pya.Box(8100 * scale_factor, 3100 * scale_factor, (8100 * scale_factor) + via_width, (3100 * scale_factor) + via_height)
via2_box_2 = pya.Box(100 * scale_factor, 3100 * scale_factor, (100 * scale_factor) + via_width, (3100 * scale_factor) + via_height)

via3_box_1 = pya.Box(8100 * scale_factor, 3100 * scale_factor, (8100 * scale_factor) + via_width, (3100 * scale_factor) + via_height)
via3_box_2 = pya.Box(100 * scale_factor, 3100 * scale_factor, (100 * scale_factor) + via_width, (3100 * scale_factor) + via_height)

mcon1_box_1 = pya.Box(8100 * scale_factor, 3100 * scale_factor, (8100 * scale_factor) + via_width, (3100 * scale_factor) + via_height)
mcon1_box_2 = pya.Box(100 * scale_factor, 3100 * scale_factor, (100 * scale_factor) + via_width, (3100 * scale_factor) + via_height)


top.shapes(via1).insert(via1_box_1)
top.shapes(via1).insert(via1_box_2)
top.shapes(via1).insert(via1_box_3)
top.shapes(via1).insert(via1_box_4)

top.shapes(via2).insert(via2_box_1)
top.shapes(via2).insert(via2_box_2)

top.shapes(via3).insert(via3_box_1)
top.shapes(via3).insert(via3_box_2)

top.shapes(mcon1).insert(mcon1_box_1)
top.shapes(mcon1).insert(mcon1_box_2)


# Create metal pads. Very large for testing purposes
top.shapes(m1).insert(pya.Box(0 * scale_factor, 0 * scale_factor, 1000 * scale_factor, 2000 * scale_factor))
top.shapes(m1pin).insert(pya.Box(0 * scale_factor, 0 * scale_factor, 1000 * scale_factor, 2000 * scale_factor))
top.shapes(m2).insert(pya.Box(0 * scale_factor, 0 * scale_factor, 1000 * scale_factor, 2000 * scale_factor))
top.shapes(m2pin).insert(pya.Box(0 * scale_factor, 0 * scale_factor, 1000 * scale_factor, 2000 * scale_factor))

top.shapes(m1).insert(pya.Box(8000 * scale_factor, 0 * scale_factor, 10000 * scale_factor, 2000 * scale_factor))
top.shapes(m1pin).insert(pya.Box(8000 * scale_factor, 0 * scale_factor, 10000 * scale_factor, 2000 * scale_factor))
top.shapes(m2).insert(pya.Box(8000 * scale_factor, 0 * scale_factor, 10000 * scale_factor, 2000 * scale_factor))
top.shapes(m2pin).insert(pya.Box(8000 * scale_factor, 0 * scale_factor, 10000 * scale_factor, 2000 * scale_factor))

top.shapes(m1).insert(pya.Box(8000 * scale_factor, 3000 * scale_factor, 10000 * scale_factor, 5000 * scale_factor))
top.shapes(m1pin).insert(pya.Box(8000 * scale_factor, 3000 * scale_factor, 10000 * scale_factor, 5000 * scale_factor))
top.shapes(m2).insert(pya.Box(8000 * scale_factor, 3000 * scale_factor, 10000 * scale_factor, 5000 * scale_factor))
top.shapes(m3).insert(pya.Box(8000 * scale_factor, 3000 * scale_factor, 10000 * scale_factor, 5000 * scale_factor))
top.shapes(m4).insert(pya.Box(8000 * scale_factor, 3000 * scale_factor, 10000 * scale_factor, 5000 * scale_factor))
top.shapes(m4pin).insert(pya.Box(8000 * scale_factor, 3000 * scale_factor, 10000 * scale_factor, 5000 * scale_factor))

top.shapes(m1).insert(pya.Box(0 * scale_factor, 3000 * scale_factor, 1000 * scale_factor, 5000 * scale_factor))
top.shapes(m1pin).insert(pya.Box(0 * scale_factor, 3000 * scale_factor, 1000 * scale_factor, 5000 * scale_factor))
top.shapes(m2).insert(pya.Box(0 * scale_factor, 3000 * scale_factor, 1000 * scale_factor, 5000 * scale_factor))
top.shapes(m3).insert(pya.Box(0 * scale_factor, 3000 * scale_factor, 1000 * scale_factor, 5000 * scale_factor))
top.shapes(m4).insert(pya.Box(0 * scale_factor, 3000 * scale_factor, 1000 * scale_factor, 5000 * scale_factor))
top.shapes(m4pin).insert(pya.Box(0 * scale_factor, 3000 * scale_factor, 1000 * scale_factor, 5000 * scale_factor))

top.shapes(li1).insert(pya.Box(8000 * scale_factor, 3000 * scale_factor, 10000 * scale_factor, 5000 * scale_factor))
top.shapes(li1).insert(pya.Box(0 * scale_factor, 3000 * scale_factor, 1000 * scale_factor, 5000 * scale_factor))


# Create pins
pin_pos = pya.Point(500 * scale_factor, 500 * scale_factor)
pin_box = pya.Box(pin_pos - pya.Point(50 * scale_factor, 50 * scale_factor), pin_pos + pya.Point(50 * scale_factor, 50 * scale_factor))

pin_pos2 = pya.Point(8500 * scale_factor, 500 * scale_factor)
pin_box2 = pya.Box(pin_pos2 - pya.Point(50 * scale_factor, 50 * scale_factor), pin_pos2 + pya.Point(50 * scale_factor, 50 * scale_factor))

top.shapes(m1).insert(pin_box)
top.shapes(m1).insert(pin_box2)

text = pya.Text("A", pya.Trans(pin_pos))
top.shapes(m2label).insert(text)

text = pya.Text("Y", pya.Trans(pin_pos2))
top.shapes(m2label).insert(text)

pin_pos3 = pya.Point(500 * scale_factor, 3500 * scale_factor)
text = pya.Text("VPWR", pya.Trans(pin_pos3))
top.shapes(m4label).insert(text)

pin_pos4 = pya.Point(8500 * scale_factor, 3500 * scale_factor)
text = pya.Text("VGND", pya.Trans(pin_pos4))
top.shapes(m4label).insert(text)


layout.write("t.gds")
print("Completed layout creation.")
