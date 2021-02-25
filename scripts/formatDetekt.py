import xml.etree.ElementTree as ET
from argparse import ArgumentParser
import subprocess

parser = ArgumentParser()
parser.add_argument("-f", "--filename", required=True,
                    help="file name to pass to detekt")
args = parser.parse_args()
filename = args.filename
reportFilename = filename + "pr.xml"

process = subprocess.Popen("./detekt -i " + filename + " -c detekt-config.yml -r xml:" + reportFilename, shell=True)
output, error = process.communicate()
if error:
    print(error)
else:
    tree = ET.parse(reportFilename)
    root = tree.getroot()

    for child in root[0]:
        print("file=" + filename + ", sev=" + child.get('severity') + ", line=" + child.get('line') + ", msg=" + child.get('message'))

    subprocess.Popen("rm " + reportFilename, shell=True)