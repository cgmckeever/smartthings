#!/usr/bin/env python

import cgi, os
from arlo import Arlo

import json
import re

def pp(data):
    print(json.dumps(data, indent=4, sort_keys=True))


print("Content-Type: application/json")
print()

USERNAME = os.environ.get('USERNAME')
PASSWORD = os.environ.get('PASSWORD')

try:
    arlo = Arlo(USERNAME, PASSWORD, "../../gmail.credentials")

    devices = arlo.GetDevices()
    for i, device in enumerate(devices):
        for key in ['deviceId', 'parentId', 'uniqueId', 'userId', 'xCloudId']:
            if key in device:
                device[key] = re.sub(r'[0-9A-Za-z]', r'X', device.get(key))

        for key in ['deviceName', 'presignedFullFrameSnapshotUrl', 'presignedLastImageUrl', 'presignedSnapshotUrl']:
            device[key] = ""

        device['owner']['ownerId'] = re.sub(r'[0-9A-Za-z]', r'X', device['owner']['ownerId'])
        device['owner']['firstName'] = ""
        device['owner']['lastName'] = ""

        devices[i] = device

    pp(devices)
except Exception as e:
    print('{"error": ' + str(e) + '}')

