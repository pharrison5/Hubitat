/**
 * Unfolded Circle Remote 3 Global Driver
 *
 * Handles global API commands applicable to the UC Remote hub itself,
 * such as reboot, setting system mode, and power_off (sleep).
 *
 * Version: 1.0.0
 * Copyright (c) 2025 Paul Harrison
 * Licensed under the MIT License
 */
metadata {
    definition(name: "Remote3GlobalDriver", namespace: "pharrison", author: "Paul Harrison") {
        capability "Refresh"
        capability "Switch" // Allows 'off()' command for power_off
        command "reboot"
        command "setMode", ["string"]
    }
}

// --- GLOBAL COMMANDS ---

def on() { 
    log.warn "The ON command is not applicable for the Global Hub entity and is ignored."
}

def off() { 
    stabilizeConnection()
    // CRITICAL: Map Hubitat 'off' to the global API power command to stop activities/sleep
    sendCommand("power_off") 
}

def reboot() { 
    stabilizeConnection()
    sendCommand("reboot") 
}

def setMode(modeName) {
    stabilizeConnection()
    sendCommand("set_mode", [mode: modeName]) 
}

def refresh() {
    stabilizeConnection() 
}

// --- UTILITY CORE ---

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
    
    log.info "GLOBAL COMMAND PAYLOAD: Method=PUT, URL=${url}, Body Map=[cmd_id:${cmd}, parameters:${params}]" 

    int maxRetries = 2
    boolean success = false
    
    for (int retry = 1; retry <= maxRetries; retry++) {
        log.debug "Command '${cmd}' attempt ${retry} for ${device.displayName}."
        try {
            httpPutJson([uri: url, headers: headers, body: bodyMap, timeout: 5]) { resp ->
                if (resp.status == 200 || resp.status == 202) { 
                    log.info "Command SUCCEEDED on attempt ${retry}! Status ${resp.status}."
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
// (Note: refreshStatusQuery is typically not needed for Global device, but can be added if required)
