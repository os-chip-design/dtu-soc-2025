# Example script that creates an input, an output, and a power/ground pin.
# No logic, just pads.
import pya
import math

cell_name = "memcellcustom"

# IO:
cell_inputs = ["WL"]
cell_outputs = ["b", "b_n"]
cell_power  = "VPWR"
cell_ground = "VGND"

# Create cell
layout = pya.Layout()
top = layout.create_cell(cell_name)

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
li1label = layout.layer(67, 5) # li1 label
licon = layout.layer(66, 44)
mcon1 = layout.layer(67, 44)
diff = layout.layer(65, 20)
nwell = layout.layer(64, 20)
poly = layout.layer(66, 20)

ndiff = layout.layer(65, 20) # Wrong, but we continue
tap = layout.layer(65, 44)

nsdm = layout.layer(93, 44)
psdm = layout.layer(94, 20)


# Sizes:
poly_width = 150
li1_width = 170
m1_width = 480
m2_width = 480
m3_width = 480
m4_width = 480

pdif_poly_space = 130

licon_dim = 170
mcon_dim = 170
via1_dim = 150
via2_dim = 200
via3_dim = 200

licon_spacing = 170
mcon_spacing = 170
via1_spacing = 170
via2_spacing = 200
via3_spacing = 200


cell_width = 5000
cell_height = 5000
diffusion_start = 3500
nwell_space = 180

ndiff_spacing = 180 # Not verified
ndiff_dist_to_nwell = 300

GND_height = 2000

# power contacts
contact_xoffset = 350

# Poly offset
poly_dist = 800

# N-type diffusion extension
ntype_len = 600
ndiff_width = 480

poly_enclosure = 80 # could be 50, but be aware of licon.8a
poly_spacing = 210 + poly_enclosure

# PDN params
PDN_width = 3100
PDN_Spacing = 15500


li_width = 300

pin_size = 100/2


def create_via_grid(block_ll, block_ur, via_dim, via_spacing, layer):

    x_coord = block_ll.x + via_spacing + via_dim/2
    y_coord = block_ll.y + via_spacing + via_dim/2

    grid_length = block_ur.x - block_ll.x
    grid_height = block_ur.y - block_ll.y
    # print(f"Grid length: {grid_length}, Grid height: {grid_height}")

    num_vias_x = math.floor((grid_length - via_spacing) / (via_spacing + via_dim))
    num_vias_y = math.floor((grid_height - via_spacing) / (via_spacing + via_dim))
    if num_vias_x < 1:
        num_vias_x = 1
        x_coord = block_ll.x + grid_length / 2 
    if num_vias_y < 1:
        num_vias_y = 1
        y_coord = block_ll.y + grid_height / 2

    # print(f"Number of vias in x: {num_vias_x}, Number of vias in y: {num_vias_y}")

    for i in range(num_vias_x):
        for j in range(num_vias_y):
            x = x_coord + i * (via_spacing + via_dim)
            y = y_coord + j * (via_spacing + via_dim)
            top.shapes(layer).insert(pya.Box(x - via_dim/2, y - via_dim/2, x + via_dim/2, y + via_dim/2))



# power rails
VCC_rail = pya.Box(0, cell_height-m1_width-pdif_poly_space, cell_width, cell_height-pdif_poly_space)
top.shapes(m1).insert(VCC_rail)

GND_rail = pya.Box(0, GND_height, cell_width, GND_height+m1_width)
top.shapes(m1).insert(GND_rail)

# P-type diffusion
middle_spacing = 750/2

p_diff1 = pya.Box(nwell_space, diffusion_start + nwell_space, cell_width/2 - nwell_space - middle_spacing, cell_height - nwell_space)
p_diff2 = pya.Box(cell_width/2 + nwell_space + middle_spacing, diffusion_start + nwell_space, cell_width - nwell_space, cell_height - nwell_space)

top.shapes(diff).insert(p_diff1)
top.shapes(psdm).insert(p_diff1)
top.shapes(diff).insert(p_diff2)
top.shapes(psdm).insert(p_diff2)

# N_well
n_well_box = pya.Box(0, diffusion_start, cell_width, cell_height)
top.shapes(nwell).insert(n_well_box)

## Contacts to net

# Power contacts
VCC_contact = pya.Box(mcon_dim)
VCC_contact.move(contact_xoffset, cell_height -m1_width/2-pdif_poly_space)
top.shapes(mcon1).insert(VCC_contact)
top.shapes(licon).insert(VCC_contact)

li_contact = pya.Box(mcon_dim*2)
li_contact.move(contact_xoffset, cell_height -m1_width/2-pdif_poly_space)
top.shapes(li1).insert(li_contact)

VCC_contact2 = pya.Box(mcon_dim)
VCC_contact2.move(cell_width - contact_xoffset, cell_height -m1_width/2-pdif_poly_space)
top.shapes(mcon1).insert(VCC_contact2)
top.shapes(licon).insert(VCC_contact2)

li_contact = pya.Box(mcon_dim*2)
li_contact.move(cell_width - contact_xoffset, cell_height -m1_width/2-pdif_poly_space)
top.shapes(li1).insert(li_contact)

# li1 cannot be the same size as the vias, min area is 0.0561 um^2

## n-well tap
tap_coords = [cell_width/2, (cell_height + diffusion_start)/2 ]

tap_box = pya.Box(tap_coords[0] - licon_dim / 2, tap_coords[1] - licon_dim / 2, tap_coords[0] + licon_dim / 2, tap_coords[1] + licon_dim / 2)
top.shapes(tap).insert(tap_box)

n_diff1_contact = pya.Box(licon_dim + 240)
n_diff1_contact.move(tap_coords[0], tap_coords[1])
top.shapes(tap).insert(n_diff1_contact)
top.shapes(nsdm).insert(n_diff1_contact)

diff1_licon_contact = pya.Box(licon_dim)
diff1_licon_contact.move(tap_coords[0], tap_coords[1])
top.shapes(licon).insert(diff1_licon_contact)

diff1_li1_contact = pya.Box(li1_width * 2)
diff1_li1_contact.move(tap_coords[0], tap_coords[1])
top.shapes(li1).insert(diff1_li1_contact)

mcon1_contact = pya.Box(mcon_dim)
mcon1_contact.move(tap_coords[0], tap_coords[1])
top.shapes(mcon1).insert(mcon1_contact)

met1_contact = pya.Box(m1_width)
met1_contact.move(tap_coords[0], tap_coords[1])
top.shapes(m1).insert(met1_contact)
# met1_gnd_connect = pya.Box(tap_coords[0] - m1_width/2, GND_height, tap_coords[0] + m1_width/2, tap_coords[1] + m1_width/2)
# top.shapes(m1).insert(met1_gnd_connect)


# GND contacts
GND_contact = pya.Box(mcon_dim)
GND_contact.move(contact_xoffset, GND_height + m1_width/2)
top.shapes(mcon1).insert(GND_contact)
top.shapes(licon).insert(GND_contact)

GND_contact2 = pya.Box(mcon_dim)
GND_contact2.move(cell_width - contact_xoffset, GND_height + m1_width/2)
top.shapes(mcon1).insert(GND_contact2)
top.shapes(licon).insert(GND_contact2)

li_contact = pya.Box(mcon_dim*2)
li_contact.move(contact_xoffset, GND_height + m1_width/2)
top.shapes(li1).insert(li_contact)

li_contact = pya.Box(mcon_dim*2)
li_contact.move(cell_width - contact_xoffset, GND_height + m1_width/2)
top.shapes(li1).insert(li_contact)

# N type diffusion
n_diff1 = pya.Box(poly_dist - ntype_len, GND_height + m1_width/2 - ndiff_width/2,  poly_dist + poly_width + ntype_len, GND_height + m1_width/2 + ndiff_width/2)
top.shapes(ndiff).insert(n_diff1)
top.shapes(nsdm).insert(n_diff1)


tap_coords = [cell_width/2, GND_height]

tap_box = pya.Box(tap_coords[0] - licon_dim / 2, tap_coords[1] - licon_dim / 2, tap_coords[0] + licon_dim / 2, tap_coords[1] + licon_dim / 2)
top.shapes(tap).insert(tap_box)

n_diff1_contact = pya.Box(licon_dim + 240)
n_diff1_contact.move(tap_coords[0], tap_coords[1])
top.shapes(tap).insert(n_diff1_contact)
top.shapes(psdm).insert(n_diff1_contact)

diff1_licon_contact = pya.Box(licon_dim)
diff1_licon_contact.move(tap_coords[0], tap_coords[1])
top.shapes(licon).insert(diff1_licon_contact)

diff1_li1_contact = pya.Box(li1_width * 2)
diff1_li1_contact.move(tap_coords[0], tap_coords[1])
top.shapes(li1).insert(diff1_li1_contact)

mcon1_contact = pya.Box(mcon_dim)
mcon1_contact.move(tap_coords[0], tap_coords[1])
top.shapes(mcon1).insert(mcon1_contact)

met1_contact = pya.Box(m1_width, m1_width)
met1_contact.move(tap_coords[0], tap_coords[1])
top.shapes(m1).insert(met1_contact)



n_diff2 = pya.Box(cell_width - poly_dist - ntype_len - poly_width, GND_height + m1_width/2 - ndiff_width/2,  cell_width + ntype_len - poly_dist, GND_height + m1_width/2 + ndiff_width/2)
top.shapes(ndiff).insert(n_diff2)
top.shapes(nsdm).insert(n_diff2)

n_diff3 = pya.Box(poly_dist + poly_width + ntype_len - li_width/2 -ndiff_width/2 , 0 , poly_dist + poly_width + ntype_len - li_width/2 +ndiff_width/2, ntype_len + poly_dist + poly_width)
top.shapes(ndiff).insert(n_diff3)
top.shapes(nsdm).insert(n_diff3)

n_diff4 = pya.Box(cell_width - poly_dist - poly_width - ntype_len + li_width/2 - ndiff_width/2 , 0 ,cell_width - poly_dist - poly_width - ntype_len + li_width/2 + ndiff_width/2, ntype_len + poly_dist + poly_width)
top.shapes(ndiff).insert(n_diff4)
top.shapes(nsdm).insert(n_diff4)



## Poly
poly_box1 = pya.Box(poly_dist, GND_height - pdif_poly_space , poly_dist + poly_width, cell_height)
poly_box2 = pya.Box(cell_width - poly_width - poly_dist, GND_height - pdif_poly_space , cell_width - poly_dist, cell_height)
top.shapes(poly).insert(poly_box1)
top.shapes(poly).insert(poly_box2)

poly_box3 = pya.Box(0, poly_dist, cell_width, poly_dist + poly_width)
top.shapes(poly).insert(poly_box3)

# poly connections 
polyc1 = pya.Box(poly_dist, GND_height + m1_width + poly_spacing, cell_width - poly_dist  -poly_width - ntype_len + li_width/2, GND_height + m1_width + poly_spacing + poly_width)
polyc2 = pya.Box(poly_dist + poly_width + ntype_len - li_width/2, GND_height + m1_width + poly_spacing*2 + poly_width,cell_width - poly_dist  -poly_width, GND_height + m1_width + poly_spacing*2 + poly_width*2)
top.shapes(poly).insert(polyc1)
top.shapes(poly).insert(polyc2)


## li1

y_end_of_li1 = poly_dist + poly_width + ntype_len - 2*licon_dim

# center measurements
x_licon1 = poly_dist + poly_width + ntype_len - li_width/2
x_licon2 = cell_width - poly_dist - poly_width - ntype_len + li_width/2

li1_1 = pya.Box(poly_dist + poly_width + ntype_len - li_width, y_end_of_li1, poly_dist + poly_width + ntype_len, cell_height)
top.shapes(li1).insert(li1_1)

li1_2 = pya.Box(cell_width - poly_dist - poly_width - ntype_len, y_end_of_li1, cell_width - poly_dist - poly_width - ntype_len + li_width, cell_height)
top.shapes(li1).insert(li1_2)

li1_3 = pya.Box(licon_dim*2)
li1_4 = pya.Box(licon_dim*2)

li1_3.move(x_licon1,  licon_dim)
li1_4.move(x_licon2,  licon_dim)

top.shapes(li1).insert(li1_3)
top.shapes(li1).insert(li1_4)



## connections from li1
licon_1 = pya.Box(licon_dim)
polybox_1 = pya.Box(licon_dim + poly_enclosure*2)

licon_1.move(x_licon2, GND_height + m1_width + poly_spacing + poly_width - licon_dim/2)
top.shapes(licon).insert(licon_1)
polybox_1.move(cell_width - poly_dist  -poly_width - ntype_len + li_width/2, GND_height + m1_width + poly_spacing + poly_width - licon_dim/2)
top.shapes(licon).insert(licon_1)
top.shapes(poly).insert(polybox_1)


licon_2 = pya.Box(licon_dim)
polybox_2 = pya.Box(licon_dim + poly_enclosure*2)

licon_2.move(x_licon1, GND_height + m1_width + poly_spacing*2 + poly_width + licon_dim/2)
top.shapes(licon).insert(licon_2)
polybox_2.move(poly_dist + poly_width + ntype_len - li_width/2, GND_height + m1_width + poly_spacing*2 + poly_width + licon_dim/2)
top.shapes(poly).insert(polybox_2)


# bottom connections
licon_3 = pya.Box(licon_dim)
licon_3.move(x_licon1, y_end_of_li1 + licon_dim)


licon_4 = pya.Box(licon_dim)
licon_4.move(x_licon2, y_end_of_li1 + licon_dim)

top.shapes(licon).insert(licon_3)
top.shapes(licon).insert(licon_4)

licon_9 = pya.Box(licon_dim)
licon_10 = pya.Box(licon_dim)

licon_9.move(x_licon1, licon_dim)
licon_10.move(x_licon2, licon_dim)

top.shapes(licon).insert(licon_9)
top.shapes(licon).insert(licon_10)

# middle connections
licon_5 = pya.Box(licon_dim)
licon_6 = pya.Box(licon_dim)
licon_7 = pya.Box(licon_dim)
licon_8 = pya.Box(licon_dim)

licon_5.move(x_licon1, GND_height + m1_width/2)
licon_6.move(x_licon1, cell_height -m1_width/2-pdif_poly_space)

licon_7.move(x_licon2, GND_height + m1_width/2)
licon_8.move(x_licon2, cell_height -m1_width/2-pdif_poly_space)




top.shapes(licon).insert(licon_5)
top.shapes(licon).insert(licon_6)
top.shapes(licon).insert(licon_7)
top.shapes(licon).insert(licon_8)

# #  nsdm and psdm
# nsdm_box = pya.Box(0,0, cell_width, diffusion_start)
# top.shapes(nsdm).insert(nsdm_box)

# psdm_box = pya.Box(0,diffusion_start, cell_width, cell_height)
# top.shapes(psdm).insert(psdm_box)


# Create pins
### Input pins
pin_in_1_pos = pya.Point(contact_xoffset, poly_dist + poly_width/2)
pin_in_1_text = pya.Text(cell_inputs[0], pya.Trans(pin_in_1_pos))
pin_in_1_box = pya.Box(pin_in_1_pos - pya.Point(pin_size, pin_size), pin_in_1_pos + pya.Point(pin_size, pin_size))

poly_pin_box = pya.Box(licon_dim + poly_enclosure*2)
licon_pin_box = pya.Box(licon_dim)
li1_pin_box = pya.Box(licon_dim*2)
m1_pin_box = pya.Box(m1_width)
mcon1_box = pya.Box(mcon_dim)

poly_pin_box.move(pin_in_1_pos)
licon_pin_box.move(pin_in_1_pos)
li1_pin_box.move(pin_in_1_pos)
m1_pin_box.move(pin_in_1_pos)
mcon1_box.move(pin_in_1_pos)

top.shapes(li1).insert(li1_pin_box)
top.shapes(licon).insert(licon_pin_box)
top.shapes(poly).insert(poly_pin_box)
top.shapes(m1label).insert(pin_in_1_text)

top.shapes(mcon1).insert(mcon1_box)
top.shapes(m1pin).insert(m1_pin_box)
top.shapes(m1).insert(m1_pin_box)
top.shapes(m1label).insert(pin_in_1_text)

### output pins
pin_out_1_pos = pya.Point(x_licon1, licon_dim)
pin_out_1_text = pya.Text(cell_outputs[0], pya.Trans(pin_out_1_pos))
pin_out_1_box = pya.Box(pin_out_1_pos - pya.Point(pin_size, pin_size), pin_out_1_pos + pya.Point(pin_size, pin_size))

pin_out_1_m1_box = pya.Box(m1_width)
pin_out_1_mcon_box = pya.Box(mcon_dim)
pin_out_1_m1_box.move(pin_out_1_pos)
pin_out_1_mcon_box.move(pin_out_1_pos)

top.shapes(m1).insert(pin_out_1_m1_box)
top.shapes(mcon1).insert(pin_out_1_mcon_box)
top.shapes(m1pin).insert(pin_out_1_m1_box)
top.shapes(m1label).insert(pin_out_1_text)



# top.shapes(li1label).insert(pin_out_1_text)
# top.shapes(li1pin).insert(pin_out_1_box)

pin_out_2_pos = pya.Point(x_licon2, licon_dim)
pin_out_2_text = pya.Text(cell_outputs[1], pya.Trans(pin_out_2_pos))
pin_out_2_box = pya.Box(pin_out_2_pos - pya.Point(pin_size, pin_size), pin_out_2_pos + pya.Point(pin_size, pin_size))

pin_out_2_m1_box = pya.Box(m1_width)
pin_out_2_mcon_box = pya.Box(mcon_dim)
pin_out_2_m1_box.move(pin_out_2_pos)
pin_out_2_mcon_box.move(pin_out_2_pos)

top.shapes(m1).insert(pin_out_2_m1_box)
top.shapes(mcon1).insert(pin_out_2_mcon_box)
top.shapes(m1pin).insert(pin_out_2_m1_box)
top.shapes(m1label).insert(pin_out_2_text)

# top.shapes(li1label).insert(pin_out_2_text)
# top.shapes(li1pin).insert(pin_out_2_box)


# Create power connections up to met4
vcc_rail_ll = pya.Point(0, cell_height-m1_width-pdif_poly_space)
vcc_rail_ur = pya.Point(cell_width, cell_height-pdif_poly_space)

GND_rail_ll = pya.Point(0, GND_height)
GND_rail_ur = pya.Point(cell_width, GND_height+m1_width)

# Create vias
create_via_grid(vcc_rail_ll, vcc_rail_ur, via1_dim, via1_spacing, via1)
create_via_grid(GND_rail_ll, GND_rail_ur, via1_dim, via1_spacing, via1)

create_via_grid(vcc_rail_ll, vcc_rail_ur, via2_dim, via2_spacing, via2)
create_via_grid(GND_rail_ll, GND_rail_ur, via2_dim, via2_spacing, via2)

create_via_grid(vcc_rail_ll, vcc_rail_ur, via3_dim, via3_spacing, via3)
create_via_grid(GND_rail_ll, GND_rail_ur, via3_dim, via3_spacing, via3)

# power planes:
vcc_rail_met2 = pya.Box(0, cell_height-m2_width-pdif_poly_space, cell_width, cell_height-pdif_poly_space)
top.shapes(m2).insert(vcc_rail_met2)

vcc_rail_met3 = pya.Box(0, cell_height-m3_width-pdif_poly_space, cell_width, cell_height-pdif_poly_space)
top.shapes(m3).insert(vcc_rail_met3)

vcc_rail_met4 = pya.Box(0, cell_height-m4_width-pdif_poly_space, cell_width + 50*PDN_width, cell_height -m4_width + PDN_width-pdif_poly_space)
top.shapes(m4).insert(vcc_rail_met4)

gnd_rail_met2 = pya.Box(0, GND_height, cell_width, GND_height+m2_width)
top.shapes(m2).insert(gnd_rail_met2)

gnd_rail_met3 = pya.Box(0, GND_height, cell_width, GND_height+m3_width)
top.shapes(m3).insert(gnd_rail_met3)

gnd_rail_met4 = pya.Box(0, GND_height + m4_width - PDN_width, cell_width + 50*PDN_width, GND_height+m4_width)
top.shapes(m4).insert(gnd_rail_met4)

top.shapes(m4pin).insert(vcc_rail_met4)
top.shapes(m4pin).insert(gnd_rail_met4)

vcc_label = pya.Text(cell_power, pya.Trans((vcc_rail_ll + vcc_rail_ur) / 2))
top.shapes(m4label).insert(vcc_label)

gnd_label = pya.Text(cell_ground, pya.Trans((GND_rail_ll + GND_rail_ur) / 2))
top.shapes(m4label).insert(gnd_label)

# Create boundary
boundary = pya.Box(0, 0, cell_width*100, cell_height*100) # To trick PDN
top.shapes(bd).insert(boundary)


layout.write(f"{cell_name}.gds")
print("Completed layout creation.")

# Write csv file for .lef

with open(f"{cell_name}_lef_config.csv", "w") as f:
    for input in cell_inputs:
        f.write(f"{input}, INPUT\n")
    for output in cell_outputs:
        f.write(f"{output}, OUTPUT\n")
    f.write(f"{cell_power}, INOUT\n")
    f.write(f"{cell_ground}, INOUT\n")
