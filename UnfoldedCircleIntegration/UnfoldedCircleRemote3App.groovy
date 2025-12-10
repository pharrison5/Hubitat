/**
 * Unfolded Circle Remote 3 Integration SmartApp
 *
 * This application handles token generation, device discovery, and managing
 * the parent/child device structure for the Unfolded Circle Remote 3 (UC Remote).
 *
 * Version: 1.0.0
 * Copyright (c) 2025 Paul Harrison
 * Licensed under the MIT License
 */
 
definition(
    name: "Unfolded Circle Remote 3 Integration",
    namespace: "pharrison",
    author: "Paul Harrison",
    description: "Multi-page dynamic discovery with WoL and automated Master Off setup.",
    category: "Integrations",
    singleInstance: true,
    iconUrl: "https://unfoldedcircle.com/wp-content/uploads/2022/09/unfolded-circle-logo.png",
    iconX2Url: "https://unfoldedcircle.com/wp-content/uploads/2022/09/unfolded-circle-logo.png"
)

preferences {
    page(name: "mainPage")
    page(name: "discoverPage")
}

// *** LIFECYCLE FUNCTIONS ***
def updated() {
    provisionGlobalController()
    // Force WoL at a stable point in the lifecycle
    wakeRemote() 
}

def provisionGlobalController() {
    String dni = "UCR3-Global"
    if (!getChildDevice(dni)) {
        addChildDevice("pharrison", "Remote3GlobalDriver", dni, [name: "Remote 3 Master Power", label: "Remote 3 Master Power"])
        log.info "DEVICE CREATED: Remote 3 Master Power (${dni}) assigned driver Remote3GlobalDriver"
    } else {
        log.debug "AUTOMATED PROVISION: Master Controller already exists, skipping creation."
    }
}
// *** END LIFECYCLE FUNCTIONS ***


def mainPage() {
    def curIP = settings?.remoteIP ?: ""
    def curPin = settings?.remotePin ?: ""
    def curMac = settings?.remoteMac

    boolean isVerified = (state.authToken && curIP) ? silentVerify() : false

    if (!isVerified && curIP && curPin) {
        requestApiKey()
        isVerified = silentVerify()
    }

    dynamicPage(name: "mainPage", title: "Remote 3 Dashboard", install: true, uninstall: true) {
        section { paragraph getConnectionStatusHeader(isVerified) }
        section("Connectivity Settings") {
            input "remoteIP", "text", title: "Remote IP Address", defaultValue: curIP, submitOnChange: true
            input "remotePin", "password", title: "Configurator PIN", defaultValue: curPin, submitOnChange: true
            input "remoteMac", "text", title: "Remote MAC Address (WoL)", defaultValue: curMac
        }

        if (settings?.remoteIP && settings?.remotePin) {
            section("Auth & Management") {
                if (isVerified) {
                    paragraph "Remote is paired and authorized."
                    input "resetConnection", "button", title: "Full Revoke & Reset Pairing"
                } else { input "requestApiKey", "button", title: "Pair Hardware" }
            }
            if (isVerified) {
                section("Entity Management") { 
                    href(name: "toDiscovery", title: "Proceed to Item Selection", page: "discoverPage") 
                }
            }
        }
    }
}

def discoverPage() {
    def rawActivities = getActivitiesLightweight()
    def rawEntities = getEntitiesLightweight()
    def installedIds = getChildDevices().collect { it.deviceNetworkId }

    Map actOptions = [:]
    if (rawActivities instanceof List) {
        rawActivities.each { act ->
            def key = act.id ?: act.entity_id ?: act.act_id
            if (key) { actOptions["${key}"] = cleanName(act.name) }
        }
    }

    Map entOptions = [:]
    if (rawEntities instanceof List) {
        rawEntities.each { ent ->
            if (ent.entity_type != "activity" && ent.features?.size() > 0) {
                def key = ent.entity_id ?: ent.id
                if (key) { entOptions["${key}"] = "${cleanName(ent.name)} (${ent.entity_type})" }
            }
        }
    }

    dynamicPage(name: "discoverPage", title: "Step 3: Item Selection") {
        section("Activities") {
            input name: "selectedActivities", type: "enum", options: actOptions, multiple: true, required: false,
                  title: "Select Activities to Import (${actOptions.size()} found)", defaultValue: actOptions.keySet().findAll { it in installedIds }
        }

        section("Entities") {
            input name: "selectedEntities", type: "enum", options: entOptions, multiple: true, required: false,
                  title: "Select Entities to Import (${entOptions.size()} found)", defaultValue: entOptions.keySet().findAll { it in installedIds }
        }
        
        section { input "createDevices", "button", title: "Install Selected Devices" }
    }
}


def provisionChildren() {
    // 1. Provision Selected Activities (Macros)
    if (settings.selectedActivities) {
        def activities = getActivitiesLightweight()
        log.info "Provisioning ${settings.selectedActivities.size()} activities..."

        settings.selectedActivities.each { selectedId ->
            def act = activities.find { 
                def currentId = it.id ?: it.entity_id // Use long ID for matching
                return selectedId.equals(currentId?.toString())
            }
            if (act) {
                // Pass the selectedId (UUID) as the DNI and the API ID
                createChild(selectedId, cleanName(act.name), "activity")
            } else {
                log.warn "Provisioning failed: Could not find metadata for Activity ID [${selectedId}]."
            }
        }
    }
    
    // 2. Provision Selected Entities (Devices)
    if (settings.selectedEntities) {
        def entities = getEntitiesLightweight()
        log.info "Provisioning ${settings.selectedEntities.size()} entities..."
        entities.each { ent ->
            def id = ent.entity_id ?: ent.id
            if (id && settings.selectedEntities.contains(id.toString())) {
                // Pass the entity ID (UUID) as the DNI and the API ID
                createChild(id.toString(), cleanName(ent.name), ent.entity_type)
            }
        }
    }
}

// Simplified createChild: Only takes three arguments now, relying on DNI for API ID
def createChild(id, label, type) {
    String dni = id.toString()
    if (!getChildDevice(dni)) {
        String driverName = (type == "activity") ? "Remote3ActivityDriver" : "Remote3EntityDriver"
        
        // No longer storing apiId in data map; DNI is used for command (in this context)
        addChildDevice("pharrison", driverName, dni, [name: "UCR-${type}", label: label])
        log.info "DEVICE CREATED: ${label} (${dni}) assigned driver ${driverName}"
    } else {
        log.debug "Child Device ${label} (${dni}) already exists, skipping creation."
    }
}

// --- NETWORK CORE & UTILS ---

def getActivitiesLightweight() {
    def all = []; 
    if (!state.authToken) return all
    
    try { 
        httpGet([
            uri: "http://${settings.remoteIP}/api/activities", 
            query: [include_options: false], 
            headers: ["Authorization": "Bearer ${state.authToken}"]
        ]) { resp -> 
            if (resp.data instanceof List) { all = resp.data } 
        } 
    } catch (e) { 
        log.debug "Activity fetch error: $e" 
    }
    return all
}

def getEntitiesLightweight() {
    def r = []; 
    if (!state.authToken) return r
    
    try { 
        httpGet([
            uri: "http://${settings.remoteIP}/api/entities", 
            query: [limit: 100, include_options: false], 
            headers: ["Authorization": "Bearer ${state.authToken}"]
        ]) { resp -> 
            if (resp.data instanceof List) { r = resp.data } 
        } 
    } catch (e) { 
        log.debug "Entity fetch error: $e" 
    }
    return r
}

def silentVerify() {
    if (!state.authToken || !settings?.remoteIP) return false
    
    try { 
        def status = false
        httpGet([
            uri: "http://${settings.remoteIP}/api/activities", 
            headers: ["Authorization": "Bearer ${state.authToken}"], 
            timeout: 4
        ]) { resp -> 
            if (resp.status == 200) status = true
        }
        return status
    } catch (e) { 
        log.debug "Verify error: $e"; 
        return false 
    }
}

def requestApiKey() {
    def auth = "Basic " + "${"web-configurator"}:${settings.remotePin}".bytes.encodeBase64().toString()
    def headers = ["Authorization": auth, "Content-Type": "application/json"]
    try {
        httpGet([uri: "http://${settings.remoteIP}/api/auth/api_keys", headers: headers, ignoreSSLIssues: true, timeout: 10]) { resp ->
            if (resp.data instanceof List) {
                def old = resp.data.find { it.name == "Hubitat" }
                if (old) { 
                    log.warn "API Key Conflict Found: Attempting to revoke key ID ${old.key_id}."
                    asynchttpDelete("handleRevoke", [uri: "http://${settings.remoteIP}/api/auth/api_keys/${old.key_id}", headers: headers, timeout: 10]); 
                    pauseExecution(2000); 
                }
            }
        }
    } catch (e) { log.error "API Key Audit/Revoke Failed: ${e.message}" }
    
    state.authToken = null
    try {
        httpPost([uri: "http://${settings.remoteIP}/api/auth/api_keys", headers: headers, body: '{"name":"Hubitat","scopes":["admin"]}', ignoreSSLIssues: true, timeout: 10]) { resp ->
            if (resp.data?.api_key) state.authToken = resp.data.api_key
        }
    } catch (e) { log.error "API Key Creation Failed (Check PIN/422 Error): ${e.message}" }
}

def cleanName(raw) {
    if (raw instanceof Map) return raw.en_US ?: raw.en ?: raw.values()?.first()?.toString() ?: "Unnamed"
    return raw.toString().replaceAll(/(?i)\(null\)/, "").trim()
}

def getConnectionStatusHeader(isVerified) {
    def color = isVerified ? "#4CAF50" : "#F44336"
    return """<div style='background-color: ${color}; color: white; padding: 10px; border-radius: 5px; text-align: center; font-weight: bold;'>${isVerified ? "CONNECTED" : "DISCONNECTED"}</div>"""
}

def appButtonHandler(btn) {
    switch (btn) {
        case "createDevices": 
            provisionChildren(); 
            break
        case "resetConnection": state.authToken = null; break
        case "requestApiKey": requestApiKey(); break
    }
}

def handleRevoke(resp, data) { log.debug "Revoke signal processed" }
