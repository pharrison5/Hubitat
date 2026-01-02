/**
 * Unfolded Circle Remote 3 Global Driver
 *
 * Handles global hub actions.
 * "Off" simulates the Hardware Power Button by turning off the active activity.
 * "Refresh" syncs status of all child activity devices.
 *
 * Version: 1.0.30
 * Copyright (c) 2025 Paul Harrison
 * Licensed under the MIT License
 */
metadata {
    definition(name: "Remote3GlobalDriver", namespace: "pharrison", author: "Paul Harrison") {
        capability "Refresh"
        capability "Switch" 
    }
}

def on() { 
    // The Global Switch doesn't really have an "On" state of its own.
    // We just set the visual state. The real state is determined by the activities.
    sendEvent(name: "switch", value: "on")
}

def off() { 
    log.info "Global Off Requested: Delegating to Activity Drivers..."
    
    // 1. Get all Child Devices from the Parent App
    def siblings = parent.getChildDevices()
    
    // 2. Find any Activity Driver that is currently ON
    def activeDriver = siblings.find { child ->
        child.typeName == "Remote3ActivityDriver" && child.currentValue("switch") == "on"
    }
    
    if (activeDriver) {
        log.info "Found Active Device: [${activeDriver.displayName}]. Sending Off Command."
        // 3. Command the SIBLING driver to turn off.
        // This leverages the code in the Activity Driver that we know works.
        activeDriver.off()
    } else {
        log.info "No Activity Drivers are currently On. Device is effectively Off."
    }
    
    // Always update global status to off
    sendEvent(name: "switch", value: "off")
}

def refresh() {
    log.info "Global Refresh: Triggering Refresh on all Child Activities..."
    
    def siblings = parent.getChildDevices()
    boolean anyOn = false
    
    siblings.each { child ->
        if (child.typeName == "Remote3ActivityDriver") {
            // Tell the child to refresh itself (it knows how to talk to the remote)
            child.refresh()
            // Check if this child is now On
            if (child.currentValue("switch") == "on") anyOn = true
        }
    }
    
    // Update Global Switch to match reality (If any activity is On, Global is On)
    def state = anyOn ? "on" : "off"
    if (device.currentValue("switch") != state) {
        sendEvent(name: "switch", value: state)
        log.info "Global Status updated to: ${state}"
    }
}
