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
 *	Author: SRPOL
 *	Date: 2019-02-18
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.zcl.DataType

metadata {
	definition (name: "Zigbee Eria Multi Button", namespace: "smartthings", author: "SmartThings", mcdSync: true, ocfDeviceType: "x.com.st.d.remotecontroller") {
		capability "Actuator"
		capability "Battery"
		capability "Button"
		capability "Holdable Button"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Health Check"

		command "refresh"

		//AduroSmart
		fingerprint inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, FCCC, 1000", outClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, FCCC, 1000", manufacturer: "AduroSmart Eria", model: "ADUROLIGHT_CSC", deviceJoinName: "Eria scene button switch V2.1", mnmn: "SmartThings", vid: "generic-4-button"
		fingerprint inClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, FCCC, 1000", outClusters: "0000, 0003, 0004, 0005, 0006, 0008, 0300, FCCC, 1000", manufacturer: "ADUROLIGHT", model: "ADUROLIGHT_CSC", deviceJoinName: "Eria scene button switch V2.0", mnmn: "SmartThings", vid: "generic-4-button"
		fingerprint inClusters: "0000, 0003, 0008, FCCC, 1000", outClusters: "0003, 0004, 0006, 0008, FCCC, 1000", manufacturer: "AduroSmart Eria", model: "Adurolight_NCC", deviceJoinName: "Eria dimming button switch V2.1", mnmn: "SmartThings", vid: "generic-4-button"
		fingerprint inClusters: "0000, 0003, 0008, FCCC, 1000", outClusters: "0003, 0004, 0006, 0008, FCCC, 1000", manufacturer: "ADUROLIGHT", model: "Adurolight_NCC", deviceJoinName: "Eria dimming button switch V2.0", mnmn: "SmartThings", vid: "generic-4-button"
	}

	tiles {
		standardTile("button", "device.button", width: 2, height: 2) {
			state "default", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
			state "button 1 pushed", label: "pushed #1", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#00A0DC"
		}

		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false) {
			state "battery", label:'${currentValue}% battery', unit:""
		}

		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
			state "default", action:"refresh", icon:"st.secondary.refresh"
		}
		main (["button"])
		details(["button", "battery", "refresh"])
	}
}

def parse(String description) {
	def map = zigbee.getEvent(description)
	log.debug "parse"

	def result = map ? map : parseAttrMessage(description)

	log.debug "Description ${description} parsed to ${result}"
	return result
}

def parseAttrMessage(description) {
	def descMap = zigbee.parseDescriptionAsMap(description)
	def map = [:]

	log.debug descMap

	if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.commandInt != 0x07 && descMap?.value) {
		map = getBatteryPercentageResult(Integer.parseInt(descMap.value, 16))
	} else if (isAduroSmartRemote()) {
		map = parseAduroSmartButtonMessage(descMap)
    }

	return map
}


def sendEventToChild(buttonNumber, event) {
	String childDni = "${device.deviceNetworkId}:$buttonNumber"
	def child = childDevices.find { it.deviceNetworkId == childDni }
	child?.sendEvent(event)
}

def getBatteryPercentageResult(rawValue) {
	log.debug 'Battery'

	def volts = rawValue / 10
	if (volts > 3.0 || volts == 0 || rawValue == 0xFF) {
		[:]
	} else {
		def result = [
				name: 'battery'
		]
		def minVolts = 2.1
		def maxVolts = 3.0
		def pct = (volts - minVolts) / (maxVolts - minVolts)
		result.value = Math.min(100, (int)(pct * 100))
		def linkText = getLinkText(device)
		result.descriptionText = "${linkText} battery was ${result.value}%"
		createEvent(result)
	}
}

def refresh() {
	return fireCommands(zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage)
		+ zigbee.readAttribute(zigbee.ONOFF_CLUSTER, switchType)
 		+ zigbee.enrollResponse())
}

private fireCommands(List commands) {
    if (commands != null && commands.size() > 0) {
        log.trace("Executing commands -- state:" + state + " commands:" + commands)
        for (String value : commands){
            sendHubCommand([value].collect {new physicalgraph.device.HubAction(it)})
        }
    }
}

def ping() {
	refresh()
}

def configure() {
	def bindings = getModelBindings(device.getDataValue("model"))
	return zigbee.onOffConfig() +
			zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage, DataType.UINT8, 30, 21600, 0x01) +
			zigbee.enrollResponse() +
			zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, batteryVoltage) + bindings
}

def installed() {
	sendEvent(name: "button", value: "pushed", isStateChange: true, displayed: false)
	sendEvent(name: "supportedButtonValues", value: supportedButtonValues.encodeAsJSON(), displayed: false)
	
	initialize()
}

def updated() {
	runIn(2, "initialize", [overwrite: true])
}

def initialize() {
	def numberOfButtons = modelNumberOfButtons[device.getDataValue("model")]
	sendEvent(name: "numberOfButtons", value: numberOfButtons, displayed: false)
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

	state.lastExecuted = 0
    
	if(!childDevices) {
		addChildButtons(numberOfButtons)
	}
	if(childDevices) {
		def event
		for(def endpoint : 1..device.currentValue("numberOfButtons")) {
			event = createEvent(name: "button", value: "pushed", isStateChange: true)
			sendEventToChild(endpoint, event)
		}
	}
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

private getModelBindings(model) {
	def bindings = []
	for(def endpoint : 1..modelNumberOfButtons[model]) {
		bindings += zigbee.addBinding(zigbee.ONOFF_CLUSTER, ["destEndpoint" : endpoint])
	}

	bindings += zigbee.addBinding(zigbee.LEVEL_CONTROL_CLUSTER, ["destEndpoint" : 2]) + 
			zigbee.addBinding(zigbee.LEVEL_CONTROL_CLUSTER, ["destEndpoint" : 3])
	return bindings
}

private Map parseAduroSmartButtonMessage(Map descMap){
	def buttonState = "pushed"
	def buttonNumber = 0
	def eventTime = 1000

	if (descMap.clusterInt == zigbee.ONOFF_CLUSTER) {
		log.debug "parseAduroSmartButtonMessage 1"
		if (descMap.command == "01") {
		    buttonNumber = 1
		} else if (descMap.command == "00") {
		    buttonNumber = 4
		} else {
		    return [:]
		}
	} else if (descMap.clusterInt == zigbee.LEVEL_CONTROL_CLUSTER) {
		log.debug "parseAduroSmartButtonMessage 2"
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
		log.debug "parseAduroSmartButtonMessage 3"
		def list2 = descMap.data
		buttonNumber = (list2[1] as int) + 1
		eventTime = 300
	}

	def lastExecutedAgo = now() - state.lastExecuted
	log.debug lastExecutedAgo 

	if (lastExecutedAgo < eventTime) {
		log.debug "skipped event"
		return [:]
	}
	state.lastExecuted = now()

	if (buttonNumber != 0) {
		def childevent = createEvent(name: "button", value: "pushed", data: [buttonNumber: 1], isStateChange: true)
		sendEventToChild(buttonNumber, childevent)

		def descriptionText = "$device.displayName button $buttonNumber was $buttonState"
		log.debug descriptionText
		state.lastExecuted = now()
		return createEvent(name: "button", value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true)
    } else {
		return [:]
	}
}

def isAduroSmartRemote(){
	((device.getDataValue("model") == "Adurolight_NCC") || (device.getDataValue("model") == "ADUROLIGHT_CSC"))
}

def getADUROSMART_SPECIFIC_CLUSTER() { 0xFCCC }

private getBatteryVoltage() { 0x0020 }
private getSwitchType() { 0x0000 }
private getSupportedButtonValues() { ["pushed"]}
private getModelNumberOfButtons() {[
		"ADUROLIGHT_CSC" : 4,
		"Adurolight_NCC" : 4
]}