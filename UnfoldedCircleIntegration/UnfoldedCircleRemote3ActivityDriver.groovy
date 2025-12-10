/**
 * Unfolded Circle Remote 3 Activity Driver
 *
 * Dedicated driver for controlling UC Remote activities (e.g., "Watch TV").
 * Optimized for stability, lifecycle control (start/off), and status synchronization.
 *
 * Version: 1.0.0
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

// --- ACTIVITY COMMANDS ---

def on() { 
    stabilizeConnection()
    sendCommand("start") // Activity Start
}

def off() { 
    stabilizeConnection()
    sendCommand("off") // Activity Stop (The required API verb)
} 

def push() { 
    stabilizeConnection()
    sendCommand("start") 
}

def refresh() {
    refreshStatusQuery() 
}

// --- UTILITY CORE (Stability and Execution) ---

def safeVerify() {
    return parent?.silentVerify() 
}

def stabilizeConnection() {
    if (!parent) {
        log.error "FATAL ERROR: Child device is missing its Parent SmartApp context. Cannot execute commands."
        throw new Exception("Device creation incomplete. Parent App link is missing.")
    }
    
    log.info "STARTING AGGRESSIVE POLLING CYCLE (Reliable Wake/Auth Check)."
    
    int maxPolls = 20
    boolean isVerified = false
    
    for (int i = 0; i < maxPolls; i++) {
        log.debug "Aggressive Poll Check ${i+1}..."
        
        if (safeVerify()) { 
            isVerified = true
            log.info "AGGRESSIVE WAKE SUCCESS: Remote is awake and authenticated after ${i} checks."
            break
        }
        pauseExecution(1000)
    }

    if (!isVerified) {
        log.error "FATAL FAILURE: Remote failed to wake up after ${maxPolls} attempts. Cannot proceed."
        throw new Exception("Remote device failed to stabilize connection for command execution.")
    }
}

def sendCommand(cmd, params = [:]) {
    def parentApp = parent
    def entityId = device.deviceNetworkId.toString().trim() 
    
    def url = "http://${parentApp.remoteIP}/api/entities/${entityId}/command" 

    def headers = [
        "Authorization": "Bearer ${parentApp.state.authToken}", 
        "Content-Type": "application/json" 
    ]
    
    def bodyMap = [
        cmd_id: cmd,
        parameters: params 
    ]
    
    log.info "FINAL COMMAND PAYLOAD: Method=PUT, URL=${url}, Body Map=[cmd_id:${cmd}, parameters:${params}]" 

    int maxRetries = 2
    boolean success = false
    
    for (int retry = 1; retry <= maxRetries; retry++) {
        log.debug "Command '${cmd}' attempt ${retry} for ${device.displayName}."
        try {
            httpPutJson([uri: url, headers: headers, body: bodyMap, timeout: 5]) { resp ->
                if (resp.status == 200 || resp.status == 202) { 
                    log.info "Command SUCCEEDED on attempt ${retry}! Status ${resp.status}."
                    
                    if (cmd in ["on", "start", "off", "stop"]) {
                        def status = (cmd in ["on", "start"]) ? "on" : "off"
                        sendEvent(name: "switch", value: status)
                    }
                    success = true
                } else {
                    log.error "Command FAILED on attempt ${retry}: Status ${resp.status}, Response: ${resp.data}"
                }
            }
        } catch (e) { 
            log.warn "Command network failure on attempt ${retry}: ${e.message}"
        }

        if (success) {
            return
        }
    }
}

def refreshStatusQuery() {
    stabilizeConnection()
    
    def parentApp = parent
    def entityId = device.deviceNetworkId.toString().trim()
    
    def url = "http://${parentApp.remoteIP}/api/entities/${entityId}"
    
    def headers = [
        "Authorization": "Bearer ${parentApp.state.authToken}"
    ]
    
    log.info "REFRESH QUERY: Method=GET, URL=${url}" 

    int maxRetries = 2
    boolean success = false

    for (int retry = 1; retry <= maxRetries; retry++) {
        log.debug "Refresh query attempt ${retry} for ${device.displayName}."
        try {
            httpGet([uri: url, headers: headers, timeout: 5]) { resp ->
                if (resp.status == 200) {
                    def data = resp.data
                    
                    def isActive = data.is_active ?: (data.attributes?.state == "ON")
                    def status = isActive ? "on" : "off"
                    
                    sendEvent(name: "switch", value: status)
                    
                    log.info "Status successfully updated on attempt ${retry} for ${device.displayName}: switch is now ${status}"
                    success = true
                } else {
                    log.error "Refresh FAILED on attempt ${retry}: Status ${resp.status}, Response: ${resp.data}"
                }
            }
        } catch (e) {
            log.warn "Refresh network failure on attempt ${retry}: ${e.message}"
        }

        if (success) {
            return
        }
    }
}
