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
* 2=top held
* 3=bottom held
* 4=bottom pressed
* 
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
    definition(name: "Lightify Dimming Switch - v2", namespace: "cgmckeever", author: "cgmckeever") {
        capability "Battery"
        capability "Button"
        capability "Refresh"
        capability "Switch Level"

        command "uiToggle"

        fingerprint profileId: "0104", 
            deviceId: "0001", 
            inClusters: "0000, 0001, 0003, 0020, 0402, 0B05", 
            outClusters: "0003, 0006, 0008, 0019", 
            manufacturer: "OSRAM", 
            model: "Lightify Dimming Switch", 
            deviceJoinName: "Lightify Dimming Switch"
    }

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
            state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
        }
    }
 }

// Message Parse
//
def parse(String msgFromST) {
    state.refreshLastRun = 0
    def descMap = zigbee.parseDescriptionAsMap(msgFromST)

    if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER
        && descMap.attrInt == batteryVoltage && descMap.value != null) {
        getBatteryResult(zigbee.convertHexToInt(descMap.value))
    } else {
        parseSwitchMessage(descMap)
    } 

    reportState()
    return reportEvent()
} 

private parseSwitchMessage(Map descMap) {
    switch (Integer.parseInt(descMap.clusterId)) {
        case 6: 
            log.info('button press event')
            handleButtonPress(descMap)
            break
        case 8: 
            log.info('button held event')
            handleButtonHeld(descMap)
            break
        case 8021:
            log.info("Networking Bind Response received")
            state.boundnetwork = true
            break
        case 8034:
            log.info("Network managment Leave Response")
            state.boundnetwork = false
            break
        default:
            log.error("Unhandled message: " + descMap)
            break
    }
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

// Event Handlers
//
private handleButtonPress(Map descMap) {
    log.info('state: ' + state.value)
    state.action = 'pushed'
    state.buttonNumber = null

    switch (descMap.command) {
        case "00": 
            log.info('bottom press')
            toggle(4)
            break
        case "01": 
            log.info('top press')
            toggle(1)
            break
        case "03":
            log.info('both pressed')
            toggle(6)
            break
        case "07": 
            log.info("Button Press Bind Response")
            break 
        default:
            log.error(getLinkText(device) + " got unknown button press command: " + descMap.command)
            break
    }
}

private handleButtonHeld(Map descMap) {
    state.action = 'held'
    state.executedCount = 0
    
    switch (Integer.parseInt(descMap.command)) {
        case 1:
            log.debug("bottom held")
            executeUntilButtonReleased([buttonNumber: 3, brightnessOffset: -20])
            break
        case 3: 
            log.debug("RELEASED")
            sendButtonEvent(state.buttonNumber, "released")
            break
        case 5:
            log.debug("top held")
            executeUntilButtonReleased([buttonNumber: 2, brightnessOffset: 20])
            break
        case 7:
            log.info("Button Held Bind Response - 7")
            state.buttonNumber = 7
            break 
        case 8:
            log.info("Button Held Bind Response - 8")
            state.buttonNumber = 8
            break 
        default:
            log.error("Unhandled button held event: " + descMap)
            state.buttonNumber = null
            break
    }
}

void executeUntilButtonReleased(Map data){
    state.executedCount = state.executedCount + 1

    if (state.action == "held") {
        log.info("Button " + data.buttonNumber + " held")
        state.level += data.brightnessOffset
        sendButtonEvent(data.buttonNumber, "held")
        runIn(1, executeUntilButtonReleased, [data: data])
    }
}

private toggle(int buttonNumber) {
    if (buttonNumber == 1) {
        log.info('toggle on')
        state.value = "on"
    } else if (buttonNumber == 4) {
        log.info('toggle off')
        state.value = "off"
    }

    sendButtonEvent(buttonNumber, "pushed")
}

private uiToggle() {
    def buttonNumber = (state.value == "on") ? 4 : 1
    toggle(buttonNumber)
    reportState()
}

// Send Events
//
private reportState() {
    sendEvent(name: 'state',           unit:'on/off', type:'state',     value: state.value)
    sendEvent(name: 'battery',         unit:"%",      type:"battery",   value: state.battery)
    sendEvent(name: 'numberOfButtons', unit:"each",   type:"count",     value: 8)
    sendEvent(name: "supportedButtonValues", value: supportedButtonValues.encodeAsJSON(), displayed: false)
    log.info("Final Level: " + state.level)
    log.info("Final State: " + state.value)
}

private Map reportEvent() {
    log.debug("Button Event")
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

private sendButtonEvent(int buttonNumber, String action) {
    if (buttonNumber) {
        log.info("Button: " + buttonNumber + " " + action + " Event Sent")
        state.buttonNumber = buttonNumber
        state.action = action 

        sendEvent(name: "button", 
            value: action, 
            data: [buttonNumber: buttonNumber], 
            displayed: false, 
            isStateChange: true)
    }
}


// Refresh
//
private refresh() {
    if (now() - (Long)state.refreshLastRun < 5000) { return }

    log.debug(device.displayName + " refresh request")
    state.refreshLastRun = now()
    fireCommands(zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage) 
        //+ zigbee.onOffRefresh() 
        + zigbee.levelRefresh() 
        + zigbee.onOffConfig() 
        + zigbee.levelConfig())
}

private fireCommands(List commands) {
    if (commands != null && commands.size() > 0) {
        log.trace("Executing commands -- state:" + state + " commands:" + commands)
        for (String value : commands){
            sendHubCommand([value].collect {new physicalgraph.device.HubAction(it)})
        }
    }
}

// Attributes
//
private getBatteryVoltage() { 0x0020 }
private getSupportedButtonValues() { ["held", "pushed", "released"] }

