metadata {
    definition(name: "Email to URL Device", namespace: "cgmckeever", author: "cgmckeever") {
        capability "Actuator"
        capability "Notification"

        command "sendMessage", [[name: "Subject", type: "STRING", description: "The subject of the message"], 
                                [name: "Message", type: "STRING", description: "The body of the message"]]
    }
}

preferences {
    input "targetURL", "text", title: "Target URL", description: "Enter the URL to send the payload", required: true
    input "targetPath", "text", title: "Target Path", description: "Enter the path", required: true
    input "emailAddress", "text", title: "Email Address", description: "Enter the email address to associate with this device", required: true
}

def sendMessage(subject, message) {
    if (!targetURL) {
        log.warn "Target URL not set. Please configure it in the device preferences."
        return
    }
    
    if (!emailAddress) {
        log.warn "Email address not set. Please configure it in the device preferences."
        return
    }

    def payload = [
        subject: subject,
        message: message,
        to: emailAddress
    ]

    def requestParams = [
        uri: targetURL,
        path: targetPath,
        contentType: "application/json",
        body: payload
    ]

    try {
        httpPost(requestParams) { response ->
            if (response.status == 200) {
                log.debug "Message sent successfully: $response.data"
            } else {
                log.warn "Failed to send message. Status: ${response.status}"
            }
        }
    } catch (Exception e) {
        log.error "Error sending message: $e"
    }
}
