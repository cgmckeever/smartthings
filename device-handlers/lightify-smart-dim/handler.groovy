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
*Button numbers:
*1=down pressed
*2=up pressed
*3=down held
*4=up held
*5=release hold
*6=both buttons held
*7=down then up pressed 
*8=up then down pressed
*/

/**
* Command reference 
* on  'catchall: 0104 0006 01 01 0140 00 3A68 01 00 0000 01 00 '
* off  'catchall: 0104 0006 01 01 0140 00 3A68 01 00 0000 00 00 '
* held up   'catchall: 0104 0008 01 01 0140 00 3A68 01 00 0000 05 00 0032'
* held down  'catchall: 0104 0008 01 01 0140 00 3A68 01 00 0000 01 00 0132'
* released   'catchall: 0104 0008 01 01 0140 00 3A68 01 00 0000 03 00 '
*/


simulator {

}

/*
* sets up fingerprint for autojoin
* sets up commands for polling and others
* sets up capabilities
* sets up variables
*/
metadata {
 definition(name: "Lightify Dimming Switch - Zigbee/Webcore", namespace: "cgmckeever", author: "cgmckeever") {
  capability "Battery"
  capability "Button"
  capability "Switch Level"
  command "refresh"
  command "toggle"
  fingerprint profileId: "0104", 
    deviceId: "0001", 
    inClusters: "0000, 0001, 0003, 0020, 0402, 0B05", 
    outClusters: "0003, 0006, 0008, 0019", 
    manufacturer: "OSRAM", 
    model: "LIGHTIFY Dimming Switch", 
    deviceJoinName: "OSRAM Lightify Dimming Switch"
 }


/*
* UI 
*/
tiles(scale: 2) {
    main "button"
        details(["button", "battery", "refresh"])
    }
    standardTile("button", "device.state", width: 6, height: 4) {
        state "off", label: 'Off', action: "toggle", icon: "st.Home.home30", backgroundColor: "#ffffff", nextState: "turningOn", decoration: "flat"
        state "on", label: "On", action: "toggle", icon: "st.Home.home30", backgroundColor: "#79b821", nextState: "turningOff", decoration: "flat"
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


/*
* Parse events into attributes.
*/
def parse(String msgFromST) {
    log.debug(msgFromST)
    if (msgFromST?.startsWith('catchall:')) {
        handleMessage(msgFromST)
    } else if (msgFromST.startsWith("read")) {
        log.debug('read:' + msgFromST)
        if (msgFromST.contains("attrId: 0000")) return state
        // TODO 
        //if (msgFromST.contains("attrId: 0020,")) return batteryHandler(zigbee.parseDescriptionAsMap(msgFromST))
    } else {
        log.error('unrecognized command:' + msgFromST)
    }
} 

/* 
* ST Message Handler
*/
def Map handleMessage(String msgFromST) {
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
            state.buttonNumber=null
            state.boundnetwork=true
            break
        case 8034:
            log.info("Network managment Leave Response!!!")
            state.buttonNumber=null
            state.boundnetwork=false
            break
        default:
            log.error("Unhandled message: " + msg)
            break
    }

    reportState()
}


/**
* Button Press Handler
*/
def Map handleButtonPress(Map msg) {
    switch (msg.command) {
        case "00": // bottom held
            toggle()
            log.info('button bottom press')
            break
        case "01": // top held
            toggle()
            log.info('button top press')
            break
        case "03": // both pressed
            // UNUSED
            log.info('both buttons pressed')
            state.buttonNumber=6
            break
        case "07": // ??
            // UNUSED
            log.info("Button Press Bind Response!!!")
            state.buttonNumber=null
            break 
        default:
            log.error(getLinkText(device) + " got unknown button press command: " + msg.command)
            break
 }
}


/*
* Button Held Handler
*/
def Map handleButtonHeld(Map msg) {
    if (state.level == null) {
        state.level = 100
    }
    switch (Integer.parseInt(msg.command)) {
        case 1:
            state.buttonNumber=3
            log.debug("button bottom held")
            state.lastHeld = "down"
            state.dimming = true
            state.brightnessOffset = -20
            executeBrightnessAdjustmentUntilButtonReleased()
            break
        case 3: // released 
            state.buttonNumber=6
            log.debug("RELEASED")
            state.dimming = false
            return state
            break
        case 5:
            state.buttonNumber=4
            log.debug("button top held")
            state.lastHeld = "up"
            state.dimming = true
            state.brightnessOffset = 20
            executeBrightnessAdjustmentUntilButtonReleased()
            break
        case 7:
            log.info("Button Held Bind Response - 7!!!")
            state.bounddimmer=true
            break 
        case 8:
            log.info("Button Held Bind Response - 8!!!")
            state.bounddimmer=true
            break 
        default:
            log.error("Unhandled button held event: " + msg)
            break
    }
return msg
}

void executeBrightnessAdjustmentUntilButtonReleased(){
    log.debug(state.level)
    if (state.dimming) {
        setLevel(state.brightnessOffset)
        runIn(1, executeBrightnessAdjustmentUntilButtonReleased)
    }
}

def setLevel(int offset){

    def int level = state.level + offset
    if (level > 100) {
        level = 100
    } else if (level < 0) {
        level = 0
    }
    state.level = level
    reportState()
}


/*
* returns a map representing important states
*/
private Map getStatus() {
 return [name: 'button',
  battery: state.battery,
  value: state.value,
  level: state.level,
  lastAction: state.lastAction,
  displayed:true,
  isStateChange: true,
  data: [buttonNumber: state.buttonNumber],
  descriptionText: "$device.displayName button $state.buttonNumber was pressed"
 ]
}


/**
 * Handles event updates.  All updates go here. 
 */
def reportState() {
    sendEvent(name: 'button',          unit:"on/off", type:"state",     value: state.value, data:[buttonNumber: state.buttonNumber])
    sendEvent(name: 'state',           unit:'on/off', type:'state',     value: state.value)
    sendEvent(name: 'battery',         unit:"%",      type:"battery",   value: state.battery)
    sendEvent(name: 'level',           unit:"%",      type:"dimmer",    value: state.level)
    sendEvent(name: 'numberOfButtons', unit:"each",   type:"count",     value: 8)
    return getStatus()
}

/**
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
* toggle
*/
def toggle() {
    if (state.value == "on") {
        state.buttonNumber=1
        state.value="off"
        log.info('toggle off')
    } else {
        state.buttonNumber=2
        state.value="on"
        log.info('toggle on')
   }
   reportState()
}

/*
* Refresh support. 
*/ 
def refresh() {
    log.debug(device.displayName + " refresh request")
    fireCommands(zigbee.readAttribute(0x0001, 0x0020) + zigbee.onOffRefresh() + zigbee.levelRefresh() + zigbee.onOffConfig() + zigbee.levelConfig() )
}
