#!/usr/bin/env python

import cgi, os
from arlo import Arlo

print("Content-Type: application/json")
print()

USERNAME = os.environ.get('USERNAME')
PASSWORD = os.environ.get('PASSWORD')

def getMode():
    modes = arlo.GetModesV2()
    return modes[0].get('activeModes')[0]

try:
    arlo = Arlo(USERNAME, PASSWORD, "../../gmail.credentials")

    query = cgi.FieldStorage()
    mode = query.getvalue('mode')

    changed = 0
    currentMode = getMode()

    if mode != None:
        basestation = arlo.GetDevice('Arlo-Main')
        arlo.CustomMode(basestation, "mode" + mode)
        setMode = getMode()

        if currentMode != setMode:
            currentMode = setMode
            changed = 1

    print('{"arlo": "' + currentMode + '", "changed": ' + str(changed) + '}')
except Exception as e:
    print('{"error": ' + str(e) + '}')