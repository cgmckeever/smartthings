/**
*    SmartThings Sylvania/Osram Lightify Dimmer Switch support
*
*    Enables Dimmable Switch via Webcore Integrations.
*  
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*
*    credits: https://github.com/adamoutler/SmartThingsPublic/blob/master/devicetypes/adamoutler/lightify-dimming-switch-zigbee.src/lightify-dimming-switch-zigbee.groovy
*/



/*
* Button numbers (arbitrary defined for WebCore):
* 1=top pressed
* 2=bottom pressed
* 3=top held
* 4=bottom held
* 5=release hold
* 6=both buttons held       // not implemented
* 7=down then up pressed    // not implemented
* 8=up then down pressed    // not implemented
*/

/*
* Command reference 
* on        'catchall: 0104 0006 01 01 0140 00 3A68 01 00 0000 01 00 '
* off       'catchall: 0104 0006 01 01 0140 00 3A68 01 00 0000 00 00 '
* held up   'catchall: 0104 0008 01 01 0140 00 3A68 01 00 0000 05 00 0032'
* held down 'catchall: 0104 0008 01 01 0140 00 3A68 01 00 0000 01 00 0132'
* released  'catchall: 0104 0008 01 01 0140 00 3A68 01 00 0000 03 00 '
*/


metadata {
    definition(name: "Lightify Dimming Switch - Zigbee/Webcore", namespace: "cgmckeever", author: "cgmckeever") {
        capability "Battery"
        capability "Button"
        capability "Switch Level"
        command "refresh"
        command "uiToggle"
        fingerprint profileId: "0104", 
        deviceId: "0001", 
        inClusters: "0000, 0001, 0003, 0020, 0402, 0B05", 
        outClusters: "0003, 0006, 0008, 0019", 
        manufacturer: "OSRAM", 
        model: "LIGHTIFY Dimming Switch", 
        deviceJoinName: "OSRAM Lightify Dimming Switch"
    }

    simulator {}

    tiles(scale: 2) {
        main (["button"])
        details(["button", "battery", "refresh"])
        standardTile("button", "device.state", width: 6, height: 4) {
            state "off", label: 'Off', action: "uiToggle", icon: "st.Home.home30", backgroundColor: "#ffffff", nextState: "turningOn", decoration: "flat"
            state "on", label: "On", action: "uiToggle", icon: "st.Home.home30", backgroundColor: "#79b821", nextState: "turningOff", decoration: "flat"
            state "turningOn", label: 'Turning on', icon: "st.Home.home30", backgroundColor: "#79b821", nextState: "on", decoration: "flat"
            state "turningOff", label: 'Turning off', icon: "st.Home.home30", backgroundColor: "#ffffff", nextState: "off", decoration: "flat"
        }
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "battery", label: 'battery ${currentValue}%'
        }
        standardTile("refresh", "device.button", decoration: "flat", width: 2, height: 2) {
            state "default", label: "", action: "refresh", icon: "st.secondary.refresh"
        }
    }
 }


/*
* Parse events into attributes.
*/
def parse(String msgFromST) {
    log.debug(msgFromST)
    if (msgFromST?.startsWith('catchall:')) {
        handleMessage(msgFromST)
    } else if (msgFromST.startsWith("read")) {
        log.debug('read: ' + msgFromST)
        def descMap = zigbee.parseDescriptionAsMap(msgFromST)
        if (descMap.clusterInt == 0x0001 && descMap.attrInt == 0x0020 && descMap.value != null) {
            getBatteryResult(zigbee.convertHexToInt(descMap.value))
        }
    } else {
        log.error('unrecognized command:' + msgFromST)
    }

    reportState()
    return getStatus()
} 


private getBatteryResult(rawValue) {
    log.debug('Check Battery')

    def volts = rawValue / 10
    if (volts > 3.0 || volts == 0 || rawValue == 0xFF) {
        state.battery = -1
    } else {
        def minVolts = 2.1
        def maxVolts = 3.0
        def pct = (volts - minVolts) / (maxVolts - minVolts)
        state.battery = Math.min(100, (int)(pct * 100))
    }
}

/* 
* ST Message Handler
*/
private handleMessage(String msgFromST) {
    state.buttonNumber = null

    Map msg = zigbee.parseDescriptionAsMap(msgFromST)
    switch (Integer.parseInt(msg.clusterId)) {
        case 6: // button press
            log.info('button press')
            handleButtonPress(msg)
            break
        case 8: // button held
            log.info('button held')
            handleButtonHeld(msg)
            break
        case 8021:
            log.info("Networking Bind Response received!!!")
            state.boundnetwork = true
            break
        case 8034:
            log.info("Network managment Leave Response!!!")
            state.boundnetwork = false
            break
        default:
            log.error("Unhandled message: " + msg)
            break
    }
}


/**
* Button Press Handler
*/
private handleButtonPress(Map msg) {
    log.info('state: ' + state.value)
    state.action = 'pushed'

    switch (msg.command) {
        case "00": // bottom press
            log.info('button bottom press')
            toggle(2)
            break
        case "01": // top press
            log.info('button top press')
            toggle(1)
            break
        case "03": // both pressed
            // UNUSED
            log.info('both buttons pressed')
            state.buttonNumber = 6
            sendEventButton()
            break
        case "07": 
            log.info("Button Press Bind Response!!!")
            state.buttonNumber = null
            break 
        default:
            log.error(getLinkText(device) + " got unknown button press command: " + msg.command)
            break
 }
}


/*
* Button Held Handler
*/
private handleButtonHeld(Map msg) {
    state.action = 'held'
    state.executed = 0
    
    switch (Integer.parseInt(msg.command)) {
        case 1:
            log.debug("button bottom held")
            
            state.buttonNumber = 4

            executeUntilButtonReleased()
            break
        case 3: // released 
            log.debug("RELEASED")

            state.action = "released"
            state.buttonNumber = 5
            break
        case 5:
            log.debug("button top held")
            
            state.buttonNumber = 3

            state.brightnessOffset = 20
            executeUntilButtonReleased()
            break
        case 7:
            log.info("Button Held Bind Response - 7!!!")
            break 
        case 8:
            log.info("Button Held Bind Response - 8!!!")
            break 
        default:
            log.error("Unhandled button held event: " + msg)
            break
    }

}

void executeUntilButtonReleased(){
    def level = state.level + state.brightnessOffset
    state.executed = state.executed + 1
    if (state.action == "held") {
        log.info("Held...")
        sendEventButton()
        runIn(1, executeUntilButtonReleased)
    }
}



private uiToggle() {
    def button = (state.value == "on") ? 2 : 1
    toggle(button)
}

private toggle(int button) {
    state.action = "pushed"
    state.buttonNumber = button
 
    if (button == 2) {
        log.info('toggle off')
        state.value = "off"
    } else {
        log.info('toggle on')
        state.value = "on"
    }

    sendEventButton()

}



/*
* returns a map representing important states
*/
private Map getStatus() {
 return [
    name: 'button',
    battery: state.battery,
    value: state.value,
    level: state.level,
    lastAction: state.action,
    displayed: true,
    isStateChange: true,
    data: [buttonNumber: state.buttonNumber],
    descriptionText: "$device.displayName button $state.buttonNumber was pressed"
 ]
}


/**
 * Handles event updates. 
 */
private reportState() {
    sendEvent(name: 'state',           unit:'on/off', type:'state',     value: state.value)
    sendEvent(name: 'battery',         unit:"%",      type:"battery",   value: state.battery)
    sendEvent(name: 'numberOfButtons', unit:"each",   type:"count",     value: 8)
    log.info("Final Level: " + state.level)
    log.info("Final State: " + state.value)
}

private sendEventButton() {
    if (state.buttonNumber) {
        log.info("Button: " + state.buttonNumber + " " + state.action + " Event Fired")
        sendEvent(name: "button", value: state.action, data: [buttonNumber: state.buttonNumber], displayed: false, isStateChange: true)
    }
}



/*
* fire commands into the hub
*/
private fireCommands(List commands) {
    if (commands != null && commands.size() > 0) {
        log.trace("Executing commands -- state:" + state + " commands:" + commands)
        for (String value : commands){
            sendHubCommand([value].collect {new physicalgraph.device.HubAction(it)})
        }
    }
}


/*
* Refresh support. 
*/ 
private refresh() {
    log.debug(device.displayName + " refresh request")
    fireCommands(zigbee.readAttribute(0x0001, 0x0020) + zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.onOffConfig() + zigbee.levelConfig())
}
