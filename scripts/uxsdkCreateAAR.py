# Copyright (c) 2018-2020 DJI
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

import git
from argparse import ArgumentParser
import subprocess as sp
import shutil
import os

parser = ArgumentParser()
parser.add_argument("-b", "--branch", required=False,
                    help="branch name for uxsdk beta")
parser.add_argument("-t", "--type", required=False,
                    help="build type for aars: debug/release ")
parser.add_argument("-o", "--output", required=False,
                    help="output directory")

sp.check_call("pwd")
args = parser.parse_args()

branchName = args.branch
buildType = args.type
outputDirectory = args.output

outputPath = "/build/outputs/aar/"
outputExtension = ".aar"


# External Repo name
directoryName = "android-uxsdk-beta"


# External Gradle Wrapper path
gradleWrapper = "../"

if branchName:
	print ("************ Checking out Branch " + branchName + " **************")
	# Internal folder config
	g = git.cmd.Git("../../" + directoryName)
	# External folder config
	#g = git.cmd.Git("../../android-uxsdk-beta")
	print(g.checkout(branchName))
	print(g.pull())

if buildType == "debug":
	print("**************** Build Type Debug *******************")
else:
	print("**************** Build Type Release *******************")
	buildType = "release"

if outputDirectory:
	outputDirectory += "/"
else:
	sp.Popen("mkdir android-uxsdk-beta", shell=True)
	outputDirectory = "./android-uxsdk-beta/"
modules =["android-uxsdk-beta-core", "android-uxsdk-beta-cameracore", "android-uxsdk-beta-accessory", "android-uxsdk-beta-map", "android-uxsdk-beta-training", "android-uxsdk-beta-visualcamera", "android-uxsdk-beta-flight"]


for module in modules:
	print ("************ Building Module " + module + " **************")
	command = './gradlew :'  + module + ':assemble'
	process = sp.Popen(command, stdout=sp.PIPE, shell=True, cwd=gradleWrapper)
	output, error = process.communicate()
	if error:
		print(error)
	if output:
		print(output)
	shutil.move("../" + module + outputPath + module + "-" + buildType + outputExtension, outputDirectory)
	os.rename(outputDirectory + module + "-" + buildType + outputExtension, outputDirectory + module + outputExtension)
print(" Outputs placed at "  +  outputDirectory)


