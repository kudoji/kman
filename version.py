#!/usr/bin/env python

#	Works as follows:
#	1) when run with NO command line parameters:
#		- checks current git branch name
#		- it suppose to be in the following template 'release-\d\.\d\.\d+', e.g. 'release-0.5.5'
#		- extracts version. In the example is '0.5.5'
#		- applies the found version to several files (check 'files_to_check' variable)
#
#	2) with command line parameter (only one accepted):
#		- takes first parameter as a version
#		- adds to it '-SNAPSHOT' suffix
#		- applies version to several files (check 'files_to_check' variable)
#
#	git branch name has priority over command line parameter
#	
#	Warning!
#		Be aware, that the script doesn't make copies.
#		But it shouldn't concert you as all code is in SCM, isn't it?
#

def modules_check(module_name):	
	module_found = True
	try:
		__import__(module_name)
	except ImportError:
		module_found = False

	if not module_found:
		print("Please, install module '{}'".format(module_name))
		exit(1)

	pass

def replace_file_data(file_name, pattern, value):
	file_mode = ""
	if not pattern:
		#	file should be replaced
		file_mode = "w"
	else:
		file_mode = "r"

	try:
		file = open(file_name, file_mode)
	except:
		print("Couldn't open file '{}'".format(file_name))
	else:
		if (file_mode == "w"):
			file.write(value)
			print("\tFile changed: '{}'".format(file_name))
		else:
			data = file.read()
			data_changed = re.sub(pattern, value, data)

			if data != data_changed:
				#	save changes to file if any
				file.close()
				file = open(file_name, "w")
				file.write(data_changed)
				print("\tFile changed: '{}'".format(file_name))
			else:
				print("\tFile kept unchanged: '{}'".format(file_name))

		file.close()

	pass

modules_check('pygit2')
modules_check('re')
modules_check('sys')
modules_check('collections')

from pygit2 import Repository
import re
import sys
import collections

files_to_check = collections.OrderedDict([
	("./VERSION1", ""), # empty value means replace whole file (delete file and put new content)
	("./pom1.xml", r'(?<=<artifactId>kman</artifactId>\n\s{4}<version>)(\d\.\d\.{0,1}\d*(-SNAPSHOT){0,1})(?=</version>)'),
	("./src/main/java/com/kudoji/kman/Kman1.java", r'(?<=public final static String KMAN_VERSION = ")(\d\.\d\.{0,1}\d*(-SNAPSHOT){0,1})(?=";)')
])


#	extract 'refs/heads/branch_name'
branch_name = Repository(".git").head.name.split("/")

branch_name = branch_name[len(branch_name) - 1]

if not branch_name:
	print("branch name is invalid")
	exit(0)

#	check are we in 'release-x.x.x' branch?
release_version = re.findall(r'^release-(\d\.\d\.{0,1}\d*)$', branch_name)
if not release_version and len(sys.argv) == 1:
	print("You are currently NOT in a release branch ({})".format(branch_name))
	print("Please, set a current version as a command line parameter")
	exit(0)

version_to_apply = "";
if release_version:
	version_to_apply = release_version[0]
elif len(sys.argv) == 2:
	version_to_apply = sys.argv[1] + "-SNAPSHOT"
else:
	#	suppose to exit in above if statement
	exit(0)

print("\nApplying '{}' version to files...\n".format(version_to_apply))
for key in files_to_check:
	replace_file_data(key, files_to_check[key], version_to_apply)

print("\nDone\n")