import csv
import sys


class Port:
    def __init__(self, name, direction):
        self.name = name
        self.dir = direction

    def lef_match(self, pin_name):
        if len(pin_name) < len(self.name):
            return False

        if pin_name[0:len(self.name)] == self.name:
            return True

    def lef_direction(self):
        return 4*" " + "DIRECTION " + self.dir + " ;\n"


def is_lef_pin(line):
    split_line = line.strip().split(' ')

    if split_line[0] == "PIN":
        return (True, split_line[1])

    return (False, "")


def main():
    if len(sys.argv) < 3:
        print("Wrong number of args")
        return 1

    port_config = sys.argv[1]
    lef_file = sys.argv[2]

    lef_out = ""
    if len(sys.argv) <= 3:
        lef_out = lef_file
    else:
        lef_out = sys.argv[3]

    ports = {}
    output = []

    with open(port_config, "r") as csvfile:
        spamreader = csv.reader(csvfile, delimiter=',')

        for row in spamreader:
            ports[row[0]] = Port(row[0], row[1].strip())

    with open(lef_file, "r") as lef:
        for line in lef:
            output.append(line)
            (is_pin, pin_name) = is_lef_pin(line)
            if is_pin:
                output.append(ports[pin_name].lef_direction())

    with open(lef_out, "w") as lef:
        lef.writelines(output)


if __name__ == "__main__":
    main()
