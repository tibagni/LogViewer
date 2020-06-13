#!/usr/bin/env python3
#
# Short script for use with git credentials.
# intended to be called by Git via GIT_ASKPASS.
#
import json
import os
from sys import argv

current_dir = os.path.dirname(os.path.abspath(__file__))
with open(os.path.join(current_dir, 'credentials.json')) as file:
    json_str = file.read()
    credentials = json.loads(json_str)

if 'username' in argv[1].lower() and 'git_username' in credentials:
    print(credentials['git_username'])
    exit()

if 'password' in argv[1].lower() and 'git_pwd' in credentials:
    print(credentials['git_pwd'])
    exit()

exit(1)
