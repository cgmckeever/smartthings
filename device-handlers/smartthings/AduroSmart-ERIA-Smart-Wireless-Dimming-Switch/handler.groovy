/**
 *  Copyright 2019 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Author: SRPOL
 *  Date: 2019-02-18
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType

metadata {
    definition (name: "Zigbee Eria Multi Button - cgm", namespace: "smartthings", author: "cgmckeever", mcdSync: true, ocfDeviceType: "x.com.st.d.remotecontroller") {
        capability "Actuator"
        capability "Battery"
        capability "Button"
        capability "Configuration"
        capability "Refresh"

        //AduroSmart
        fingerprint inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, FCCC, 1000", outClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, FCCC, 1000", manufacturer: "AduroSmart Eria", model: "ADUROLIGHT_CSC", deviceJoinName: "Eria scene button switch V2.1", mnmn: "SmartThings", vid: "generic-4-button"
        fingerprint inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, FCCC, 1000", outClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, FCCC, 1000", manufacturer: "ADUROLIGHT", model: "ADUROLIGHT_CSC", deviceJoinName: "Eria scene button switch V2.0", mnmn: "SmartThings", vid: "generic-4-button"
        fingerprint inClusters: "0000, 0003, 0008, FCCC, 1000", outClusters: "0003, 0004, 0006, 0008, FCCC, 1000", manufacturer: "AduroSmart Eria", model: "Adurolight_NCC", deviceJoinName: "Eria dimming button switch V2.1", mnmn: "SmartThings", vid: "generic-4-button"
        fingerprint inClusters: "0000, 0003, 0008, FCCC, 1000", outClusters: "0003, 0004, 0006, 0008, FCCC, 1000", manufacturer: "ADUROLIGHT", model: "Adurolight_NCC", deviceJoinName: "Eria dimming button switch V2.0 - cgm", mnmn: "SmartThings", vid: "generic-4-button"
    }

    tiles {
        standardTile("button", "device.button", width: 2, height: 2) {
            state "default", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
            state "button 1 pushed", label: "pushed #1", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#00A0DC"
        }

        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, , width: 2, height: 1) {
            state "battery", label:'${currentValue}% battery'
        }

        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
            state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
            state "default", action:"Configuration.configure", icon:"st.secondary.tools"
        }

        main (["button"])
        details(["button", "configure", "refresh", "battery"])
    }
}

// Events
//
def parse(String description) {
    def map = zigbee.getEvent(description)
    def result = map ? map : parseAttrMessage(description)
    if (result.name == "switch") {
        result = createEvent(descriptionText: "Wake up event came in", isStateChange: true)
    }
    log.debug "Description ${description} parsed to ${result}"
    return result
}

def parseAttrMessage(description) {
    def map = [:]
    def descMap = zigbee.parseDescriptionAsMap(description)

    if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER) {
        map = getBatteryPercentageResult(Integer.parseInt(descMap.value, 16))
    } else {
        map = parseAduroSmartButtonMessage(descMap)
    } 

    log.debug descMap?.clusterInt
    return map
}

private Map parseAduroSmartButtonMessage(Map descMap){
    def buttonState = "pushed"
    def buttonNumber = 0

    log.debug descMap.clusterInt

    if (descMap.clusterInt == zigbee.ONOFF_CLUSTER) {
        log.debug "zigbee.ONOFF_CLUSTER"
        if (descMap.command == "01") {
            buttonNumber = 1
        } else if (descMap.command == "00") {
            buttonNumber = 4
        } else {
            return [:]
        }
    } else if (descMap.clusterInt == zigbee.LEVEL_CONTROL_CLUSTER) {
        // does this ever trigger?
        log.debug "zigbee.LEVEL_CONTROL_CLUSTER"
        if (descMap.command == "02") {
            def data = descMap.data
            def d0 = data[0]
            if (d0 == "00") {
                buttonNumber = 2
            }else if (d0 == "01") {
                buttonNumber = 3
            }
        }
    } else if (descMap.clusterInt == ADUROSMART_SPECIFIC_CLUSTER) {
        log.debug "ADUROSMART_SPECIFIC_CLUSTER"
        def list2 = descMap.data
        def button = (list2[1] as int) + 1
        if (button == 2 || button == 3) { buttonNumber = button }
    }

    if (buttonNumber != 0) {
        def childevent = createEvent(name: "button", value: "pushed", data: [buttonNumber: 1], isStateChange: true)
        sendEventToChild(buttonNumber, childevent)

        def descriptionText = "$device.displayName button $buttonNumber was $buttonState"
        return createEvent(name: "button", value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
    } else {
        return [:]
    }
}

private getBatteryPercentageResult(rawValue) {
    log.debug 'Battery'
}


private sendEventToChild(buttonNumber, event) {
    String childDni = "${device.deviceNetworkId}:$buttonNumber"
    def child = childDevices.find { it.deviceNetworkId == childDni }
    child?.sendEvent(event)
}

// Install / Configure
//
def refresh() {
    log.debug "refresh"
    return zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage)
        + zigbee.readAttribute(zigbee.ONOFF_CLUSTER, switchType)
        + zigbee.enrollResponse()
}

def ping() {
    refresh()
}

def configure() {
    log.debug "configure"
    return zigbee.onOffConfig() 
        + zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage, DataType.UINT8, 30, 21600, 0x01)
        + zigbee.enrollResponse() 
        + zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage)
        + getModelBindings(device.getDataValue("model"))
}

def installed() {
    initialize()
}

def updated() {
    runIn(2, "initialize", [overwrite: true])
}

def initialize() {
    def numberOfButtons = modelNumberOfButtons[device.getDataValue("model")]
    sendEvent(name: "numberOfButtons", value: numberOfButtons, displayed: false)
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    sendEvent(name: "supportedButtonValues", value: supportedButtonValues.encodeAsJSON(), displayed: false)

    addChildButtons(numberOfButtons)
}

private addChildButtons(numberOfButtons) {
    for(def endpoint : 1..numberOfButtons) {
        try {
            String childDni = "${device.deviceNetworkId}:$endpoint"
            def child = addChildDevice("Child Button", childDni, device.getHub().getId(), [
                    completedSetup: true,
                    label         : device.displayName + " - ${endpoint}",
                    isComponent   : true,
                    componentName : "button$endpoint",
                    componentLabel: "Button $endpoint"
            ])
            child.sendEvent(name: "supportedButtonValues", value: supportedButtonValues.encodeAsJSON(), displayed: false)
        } catch(Exception e) {
            log.debug "Exception: ${e}"
        }
    }
}

// Attributes
//
private getADUROSMART_SPECIFIC_CLUSTER() {0xFCCC}
private getBatteryVoltage() { 0x0020 }
private getSwitchType() { 0x0000 }

private getModelBindings(model) {
    def bindings = []
    bindings += zigbee.addBinding(zigbee.ONOFF_CLUSTER, ["destEndpoint" : 1]) + 
        zigbee.addBinding(zigbee.ONOFF_CLUSTER, ["destEndpoint" : 4])
    bindings += zigbee.addBinding(zigbee.LEVEL_CONTROL_CLUSTER, ["destEndpoint" : 2]) + 
        zigbee.addBinding(zigbee.LEVEL_CONTROL_CLUSTER, ["destEndpoint" : 3])
    return bindings
}

private getModelNumberOfButtons() {[
        "ADUROLIGHT_CSC" : 4,
        "Adurolight_NCC" : 4
]}

private getSupportedButtonValues() { ["pushed"] }
