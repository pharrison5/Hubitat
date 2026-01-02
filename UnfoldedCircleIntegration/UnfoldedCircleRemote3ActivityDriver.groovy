/**
 * Unfolded Circle Remote 3 Activity Driver
 *
 * Dedicated driver for controlling UC Remote activities (e.g., "Watch TV").
 * Optimized for stability, lifecycle control (start/off), and status synchronization.
 *
 * Version: 1.0.25
 * Copyright (c) 2025 Paul Harrison
 * Licensed under the MIT License
 */
metadata {
    definition(name: "Remote3ActivityDriver", namespace: "pharrison", author: "Paul Harrison") {
        capability "Switch"
        capability "Momentary"
        capability "Refresh"
    }
}

// --- COMMAND IMPLEMENTATIONS ---

def on() { 
    stabilizeConnection()
    sendCommand("start") 
}

def off() { 
    stabilizeConnection()
    sendCommand("stop") 
} 

def push() { 
    stabilizeConnection()
    sendCommand("start") 
}

def refresh() {
    stabilizeConnection()
    refreshStatusQuery() 
}

// --- STABILITY & EXECUTION CORE ---

def stabilizeConnection() {
    def parentApp = parent
    
    log.info "STARTING AGGRESSIVE POLLING CYCLE (Reliable Wake/Auth Check)."
    
    int maxPolls = 20 // Max attempts over 20 seconds
    boolean isVerified = false
    
    for (int i = 0; i < maxPolls; i++) {
        log.debug "Aggressive Poll Check ${i+1}..."
        
        // Check connectivity AND authentication simultaneously (using parent's silentVerify)
        if (parentApp.silentVerify()) {
            isVerified = true
            log.info "AGGRESSIVE WAKE SUCCESS: Remote is awake and authenticated after ${i} checks."
            break
        }
        
        // Wait 1 second before the next check
        pauseExecution(1000)
    }

    if (!isVerified) {
        log.error "FATAL FAILURE: Remote failed to wake up after ${maxPolls} attempts. Cannot proceed."
        throw new Exception("Remote device failed to stabilize connection for command execution.")
    }
}

def sendCommand(cmd) {
    def parentApp = parent
    def entityId = device.deviceNetworkId.toString().trim() 
    
    // CRITICAL FIX: Map the internal 'stop' command to the required API verb 'off'.
    def apiCmd = (cmd == "stop") ? "off" : cmd

    // Confirmed final structure: PUT to /api/entities/{id}/command
    def url = "http://${parentApp.remoteIP}/api/entities/${entityId}/command" 

    def headers = [
        "Authorization": "Bearer ${parentApp.state.authToken}", 
        "Content-Type": "application/json" 
    ]
    
    // Final correct payload structure: cmd_id and parameters
    def bodyMap = [
        cmd_id: apiCmd,
        parameters: [:]
    ]
    
    log.info "FINAL COMMAND PAYLOAD: Method=PUT, URL=${url}, Body Map=[cmd_id:${apiCmd}, parameters:[:]]" 

    int maxRetries = 2
    boolean success = false // Synchronous exit flag
    
    for (int retry = 1; retry <= maxRetries; retry++) {
        log.debug "Command '${apiCmd}' attempt ${retry} for ${device.displayName}."
        try {
            // Use httpPutJson for reliable encoding and PUT method
            httpPutJson([uri: url, headers: headers, body: bodyMap, timeout: 5]) { resp ->
                if (resp.status == 200 || resp.status == 202) { 
                    log.info "Command SUCCEEDED on attempt ${retry}! Status ${resp.status}."
                    if (apiCmd in ["on", "start"]) sendEvent(name: "switch", value: "on")
                    if (apiCmd in ["off", "stop"]) sendEvent(name: "switch", value: "off")
                    success = true
                } else {
                    log.error "Command FAILED on attempt ${retry}: Status ${resp.status}, Response: ${resp.data}"
                }
            }
        } catch (e) { 
            log.warn "Command network failure on attempt ${retry}: ${e.message}"
        }

        if (success) {
            return // Synchronous exit
        }
    }
}


def refreshStatusQuery() {
    def parentApp = parent
    def entityId = device.deviceNetworkId.toString().trim()
    
    // Confirmed path for reading state
    def url = "http://${parentApp.remoteIP}/api/entities/${entityId}"
    
    def headers = [
        "Authorization": "Bearer ${parentApp.state.authToken}"
    ]
    
    log.info "REFRESH QUERY: Method=GET, URL=${url}" 

    int maxRetries = 2
    boolean success = false // Synchronous exit flag

    for (int retry = 1; retry <= maxRetries; retry++) {
        log.debug "Refresh query attempt ${retry} for ${device.displayName}."
        try {
            httpGet([uri: url, headers: headers, timeout: 5]) { resp ->
                if (resp.status == 200) {
                    def data = resp.data
                    
                    // Check for standard state fields: 'is_active' or the attribute 'state'
                    def isActive = data.is_active ?: (data.attributes?.state == "ON")
                    def status = isActive ? "on" : "off"
                    
                    sendEvent(name: "switch", value: status)
                    log.info "Status successfully updated on attempt ${retry} for ${device.displayName}: switch is now ${status}"
                    success = true // Set the flag
                } else {
                    log.error "Refresh FAILED on attempt ${retry}: Status ${resp.status}, Response: ${resp.data}"
                }
            }
        } catch (e) {
            log.warn "Refresh network failure on attempt ${retry}: ${e.message}"
        }

        if (success) {
            return // Synchronous exit
        }
    }
}
