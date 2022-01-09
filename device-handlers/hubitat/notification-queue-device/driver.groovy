metadata {
    definition (name: "Virtual Notification Queue Device", namespace: "cgmckeever", author: "cgmckeever") {
        capability "Notification"
        capability "Actuator"
    }

   preferences {
       input( name: "nodeRedAddr",type:"string",title: "Node-RED server address", description:"The location of the Node-RED server including port #.", defaultValue:"http://[Node-RED ip address]:[port]")
       input( name: "nodeRedPath",type:"string",title: "Node-RED path", description:"", defaultValue:"/notify")
       input( name: "notifyCommand",type:"string",title: "Notify Command", description:"speak or play", defaultValue:"speak")
       input(name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true)
       input(name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true)
   }
}

void installed() {
   log.debug "installed()"
   initialize()
}

void updated() {
   log.debug "updated()"
   initialize()
}

void initialize() {
   log.debug "initialize()"
   Integer disableTime = 1800
   if (logEnable) {
      log.debug "Debug logging will be automatically disabled in ${disableTime} seconds"
      runIn(disableTime, "debugOff")
   }
}

void debugOff() {
   log.warn "Disabling debug logging"
   device.updateSetting("logEnable", [value:"false", type:"bool"])
}

void myAsynchttpHandler(resp, data) {
   if (logEnable) log.debug "HTTP ${resp.status}"
   // whatever you might need to do here (check for errors, etc.),
   if (logEnable) log.debug "HTTP ${resp.body}"
}

void deviceNotification(notificationText) {
    if (logEnable) log.debug "deviceNotification(notificationText = ${notificationText})"
    sendEvent(name: "deviceNotification", value: notificationText, isStateChange: true)

   Map params = [
      uri:  nodeRedAddr,
      contentType: "application/json",
      path: nodeRedPath,
      body: [param: notificationText, type: notifyCommand],
      timeout: 15
   ]
   asynchttpPost("myAsynchttpHandler", params)
}