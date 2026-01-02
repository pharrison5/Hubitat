/**
 * Unfolded Circle Remote 3 Integration SmartApp
 *
 * This application handles token generation, device discovery, and managing
 * the parent/child device structure for the Unfolded Circle Remote 3 (UC Remote).
 *
 * Version: 1.0.59
 * Copyright (c) 2025 Paul Harrison
 * Licensed under the MIT License
 */

definition(
    name: "Unfolded Circle Remote 3 Integration",
    namespace: "pharrison",
    author: "Paul Harrison",
    description: "Multi-page dynamic discovery with automated setup flow.",
    category: "Integrations",
    singleInstance: true,
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    page(name: "mainPage")
    page(name: "discoverPage")
}

// =============================================================================
// SECTION 1: PAGES (UI DEFINITIONS)
// =============================================================================

def mainPage() {
    boolean isInstalled = (app.installationState == "COMPLETE")
    
    // --- DRIVER INTEGRITY CHECK ---
    def missingDriversList = verifyDrivers()
    boolean driversMissing = (missingDriversList.size() > 0)
    
    def curIP = settings?.remoteIP ?: ""
    def curPin = settings?.remotePin ?: ""
    
    // Logic: Attempt connection only if settings changed or credentials exist but no token
    boolean attemptConnect = false
    if ((curIP != state.lastIP || curPin != state.lastPin) || (curIP && curPin && !state.authToken)) {
        attemptConnect = true
        state.lastIP = curIP
        state.lastPin = curPin
        state.connError = null 
    }
    
    if (attemptConnect && curIP && curPin) {
        requestApiKey()
    }
    
    boolean isVerified = false
    if (state.authToken && curIP) {
        isVerified = silentVerify()
    }
    
    String status = "disconnected"
    if (isVerified) {
        status = "connected"
        state.connError = null
    } else if (curIP && curPin) {
        status = "connecting"
    }
    
    // Redirect to discovery ONLY if drivers are OK and we are connected
    if (isInstalled && isVerified && !driversMissing) {
        return discoverPage() 
    }
    
    return initialSetupPage(status, isVerified, missingDriversList)
}

def initialSetupPage(status, isVerified, missingDriversList) {
    dynamicPage(name: "mainPage", title: "", install: (isVerified && missingDriversList.isEmpty()), uninstall: true) {
        
        section {
            boolean showError = (state.connError != null)
            boolean showSuccess = (isVerified && missingDriversList.isEmpty())
            boolean showDriverError = (missingDriversList.size() > 0)
            
            // Determine styling based on what messages are shown
            boolean headRoundBot = (!showError && !showSuccess && !showDriverError)
            
            String finalHtml = """
            <div style='display: flex; flex-direction: column; gap: 0;'>
                ${getHeaderHTML(status, true, headRoundBot)}
            """
            
            // 1. DRIVER MISSING ERROR (Highest Priority)
            if (showDriverError) {
                String missingStr = missingDriversList.collect { "• <b>${it}</b>" }.join("<br>")
                finalHtml += """
                <div style='color: #B71C1C; background-color: #FFEBEE; border-left: 1px solid #B71C1C; border-right: 1px solid #B71C1C; border-bottom: 1px solid #B71C1C; border-top: 1px solid #fff; padding: 10px; border-bottom-left-radius: 4px; border-bottom-right-radius: 4px;'>
                    <table style='width: 100%; border-collapse: collapse; margin: 0; padding: 0;'>
                        <tr><td style='padding: 0 0 5px 0; font-weight: bold; font-size: 1.1em; text-align: left;'>🛑 Missing Drivers</td></tr>
                        <tr><td style='padding: 0 0 5px 0; text-align: left;'>The App cannot run because the following Drivers are not installed in Hubitat "Drivers Code":</td></tr>
                        <tr><td style='padding: 0 0 5px 20px; text-align: left; color: #D32F2F;'>${missingStr}</td></tr>
                        <tr><td style='padding: 5px 0 0 0; font-size: 0.9em; text-align: left;'>Please go to "Drivers Code", create these drivers, and refresh this page.</td></tr>
                    </table>
                </div>"""
            }
            // 2. Connection Error Box
            else if (showError) {
                finalHtml += """
                <div style='color: #B71C1C; background-color: #FFEBEE; border-left: 1px solid #B71C1C; border-right: 1px solid #B71C1C; border-bottom: 1px solid #B71C1C; border-top: 1px solid #fff; padding: 10px; border-bottom-left-radius: 4px; border-bottom-right-radius: 4px;'>
                    <table style='width: 100%; border-collapse: collapse; margin: 0; padding: 0;'>
                        <tr><td style='padding: 0 0 5px 0; font-weight: bold; font-size: 1.1em; text-align: left;'>🛑 Connection Failed</td></tr>
                        <tr><td style='padding: 0 0 0 0; text-align: left;'>${state.connError}</td></tr>
                    </table>
                </div>"""
            }
            // 3. Success/Install Prompt
            else if (showSuccess) {
                finalHtml += """
                <div style='color: #1B5E20; background-color: #E8F5E9; border-left: 1px solid #2E7D32; border-right: 1px solid #2E7D32; border-bottom: 1px solid #2E7D32; border-top: 1px solid #fff; padding: 10px; border-bottom-left-radius: 4px; border-bottom-right-radius: 4px;'>
                    <table style='width: 100%; border-collapse: collapse; margin: 0; padding: 0;'>
                        <tr><td style='padding: 0 0 5px 0; font-weight: bold; font-size: 1.1em; text-align: left;'>✅ Connection Successful!</td></tr>
                        <tr><td style='padding: 0 0 5px 0; text-align: left;'>Drivers verified. Please click <b>"Done"</b> below to finish installing.</td></tr>
                    </table>
                </div>"""
            }
            
            finalHtml += "</div>"
            paragraph finalHtml
        }
        
        section("Connectivity Settings") {
            paragraph "Enter the IP address and PIN found in <b>Settings > About > System</b>.<br><i>Type the value and press 'Enter' (or click outside the box) to save and verify.</i>"
            input "remoteIP", "text", title: "Remote IP Address", defaultValue: settings?.remoteIP, submitOnChange: true
            input "remotePin", "password", title: "Configurator PIN", defaultValue: settings?.remotePin, submitOnChange: true
        }
        
        if (settings?.remoteIP && settings?.remotePin && !isVerified) {
            section {
                input "requestApiKey", "button", title: "Connect & Verify" 
            }
        }
        
        section("Advanced Options", hideable: true, hidden: true) {
            input "enableDebug", "bool", title: "Enable Debug Logging", defaultValue: true
        }

        // DONATION FOOTER
        section {
            paragraph getFooterHTML()
        }
    }
}

def discoverPage() {
    if (app.installationState != "COMPLETE") return mainPage()

    def rawActivities = getActivitiesLightweight()
    if (rawActivities == null) {
        pauseExecution(2000)
        rawActivities = getActivitiesLightweight()
    }

    boolean fetchSuccess = (rawActivities != null)
    String status = fetchSuccess ? "connected" : "disconnected"
    
    def rawEntities = (fetchSuccess) ? getEntitiesLightweight() : null
    
    def installedChildren = getChildDevices()
    def installedIds = installedChildren.collect { it.deviceNetworkId }

    Map actOptions = [:]
    if (rawActivities instanceof List) {
        rawActivities.each { act ->
            def key = act.id ?: act.entity_id
            if (key) { actOptions["${key}"] = cleanName(act.name) }
        }
    }

    Map mediaOptions = [:]
    Map homeOptions = [:]
    
    if (rawEntities instanceof List) {
        rawEntities.each { ent ->
            if (ent.entity_type != "activity" && ent.features?.size() > 0) {
                def key = ent.entity_id ?: ent.id
                def name = "${cleanName(ent.name)} (${ent.entity_type})"
                
                if (ent.entity_type == "media_player") {
                    mediaOptions["${key}"] = name
                } else {
                    homeOptions["${key}"] = name
                }
            }
        }
    }
    
    state.allActKeys = actOptions.keySet() as List
    state.allMediaKeys = mediaOptions.keySet() as List
    state.allHomeKeys = homeOptions.keySet() as List

    dynamicPage(name: "discoverPage", title: "", install: true, uninstall: true) {
        
        section { 
            boolean showError = (state.syncWarnings && state.syncWarnings instanceof List && state.syncWarnings.size() > 0)
            boolean showStatus = (state.syncMsg != null && (fetchSuccess || state.syncMsg.contains("Failed")))
            boolean showFetchErr = (!fetchSuccess)
            
            boolean headRoundBot = (!showError && !showStatus && !showFetchErr)
            boolean errRoundBot = (!showStatus) 
            
            String finalHtml = """
            <div style='display: flex; flex-direction: column; gap: 0;'>
                ${getHeaderHTML(status, true, headRoundBot)}
            """
            
            if (showFetchErr) {
                 finalHtml += """
                <div style='color: #B71C1C; background-color: #FFEBEE; border: 1px solid #B71C1C; border-top: 0; padding: 10px; border-bottom-left-radius: 4px; border-bottom-right-radius: 4px;'>
                    <table style='width: 100%; border-collapse: collapse; margin: 0; padding: 0;'>
                        <tr><td style='padding: 0 0 5px 0; font-weight: bold; font-size: 1.1em; text-align: left;'>🛑 Data Retrieval Failed</td></tr>
                        <tr><td style='padding: 0 0 5px 0; text-align: left;'>Remote is unreachable. Attempting to wake it up...</td></tr>
                        <tr><td style='padding: 0 0 0 0; font-size: 0.9em; text-align: left;'>Please wait a moment and click "Retry Connection" below.</td></tr>
                    </table>
                </div>"""
            }
            
            if (showError) {
                String deviceListStr = state.syncWarnings.collect { "• <b>${it}</b>" }.join("<br>")
                String radiusStyle = errRoundBot ? "border-bottom-left-radius: 4px; border-bottom-right-radius: 4px;" : ""
                
                finalHtml += """
                <div style='background-color: #FFEBEE; border-left: 1px solid #B71C1C; border-right: 1px solid #B71C1C; border-bottom: 1px solid #B71C1C; border-top: 1px solid #fff; color: #B71C1C; padding: 5px 10px; ${radiusStyle}'>
                    <table style='width: 100%; border-collapse: collapse; margin: 0; padding: 0;'>
                        <tr><td style='padding: 5px 0 15px 0; font-weight: bold; font-size: 1.1em; text-align: left;'>🛑 Error: Orphaned Devices Detected</td></tr>
                        <tr><td style='padding: 0 0 5px 0; text-align: left;'>The following devices already exist on your Hub (leftover from a previous install):</td></tr>
                        <tr><td style='padding: 0 0 15px 20px; line-height: 1.4; text-align: left;'>${deviceListStr}</td></tr>
                        <tr><td style='border-top: 1px solid #e0b4b4; padding: 10px 0 5px 0; font-weight: bold; text-align: left;'>To fix this:</td></tr>
                        <tr><td style='padding: 0 0 2px 20px; text-align: left;'>1. Go to the <b>Devices</b> page, search for these names, and delete them manually.</td></tr>
                        <tr><td style='padding: 0 0 0 20px; text-align: left;'>2. Return here and click <b>Sync Devices</b> again.</td></tr>
                    </table>
                </div>
                """
                state.syncWarnings = null 
            }
            
            if (showStatus) {
                def color = "#2E7D32" 
                if (state.syncMsg.contains("Failed")) color = "#B71C1C" 
                else if (state.syncMsg.contains("Partial")) color = "#E65100" 
                
                finalHtml += """
                <div style='color: ${color}; font-weight: bold; border-left: 1px solid ${color}; border-right: 1px solid ${color}; border-bottom: 1px solid ${color}; border-top: 1px solid #fff; padding: 8px; border-bottom-left-radius: 4px; border-bottom-right-radius: 4px;'>
                    <table style='width: 100%; border-collapse: collapse; margin: 0; padding: 0;'>
                        <tr><td style='padding: 0; text-align: left;'>${state.syncMsg}</td></tr>
                    </table>
                </div>"""
                state.syncMsg = null 
            }
            
            finalHtml += "</div>"
            paragraph finalHtml
        }

        if (fetchSuccess) {
            section {
                paragraph "<div style='font-size: 1.3em; font-weight: bold; border-bottom: 2px solid #4CAF50; margin-bottom: 5px;'>📺 Activities</div>"
                input "btnSelectAllAct", "button", title: "Select All", width: 2
                input "btnClearAct", "button", title: "Clear All", width: 2
                input name: "selectedActivities", type: "enum", options: actOptions, multiple: true, required: false,
                      title: "Select devices to import:", defaultValue: actOptions.keySet().findAll { it in installedIds }, submitOnChange: true
            }

            section {
                paragraph "<div style='font-size: 1.3em; font-weight: bold; border-bottom: 2px solid #2196F3; margin-bottom: 5px; margin-top: 10px;'>🔈 Media Players</div>"
                input "btnSelectAllMedia", "button", title: "Select All", width: 2
                input "btnClearMedia", "button", title: "Clear All", width: 2
                input name: "selectedMediaEntities", type: "enum", options: mediaOptions, multiple: true, required: false,
                      title: "Select devices to import:", defaultValue: mediaOptions.keySet().findAll { it in installedIds }, submitOnChange: true
            }

            section {
                paragraph "<div style='font-size: 1.3em; font-weight: bold; border-bottom: 2px solid #FF9800; margin-bottom: 5px; margin-top: 10px;'>🏠 Smart Home & Other</div>"
                input "btnSelectAllHome", "button", title: "Select All", width: 2
                input "btnClearHome", "button", title: "Clear All", width: 2
                input name: "selectedHomeEntities", type: "enum", options: homeOptions, multiple: true, required: false,
                  title: "Select devices to import:", defaultValue: homeOptions.keySet().findAll { it in installedIds }, submitOnChange: true
            }
            
            section {
                paragraph "<hr>"
                input "forceNameSync", "bool", title: "Update device names from Remote?", defaultValue: false, description: "Overwrites Hubitat device labels with current Remote 3 names."
                paragraph "<small>Note: Unchecking installed devices above will remove them from Hubitat.</small>"
                input "syncDevices", "button", title: "Sync Devices (Add/Remove/Update)" 
            }
        } else {
             section {
                 input "requestApiKey", "button", title: "Retry Connection" 
             }
        }
        
        section("Advanced Options", hideable: true, hidden: true) {
            input "enableDebug", "bool", title: "Enable Debug Logging", defaultValue: true, submitOnChange: true
            paragraph "<hr>" 
            input "resetConnection", "button", title: "Reset Connection & Go Back" 
        }

        // DONATION FOOTER
        section {
            paragraph getFooterHTML()
        }
    }
}

// =============================================================================
// SECTION 2: APP LOGIC
// =============================================================================

def updated() {
    // FIX APPLIED: Try/Catch wrapper preventing crash if global driver missing
    try {
        provisionGlobalController(null)
    } catch (e) {
        log.error "CRITICAL: Could not create Global Driver. Is 'Remote3GlobalDriver' installed? Error: ${e.message}"
    }
}

def installed() {
    updated()
}

def appButtonHandler(btn) {
    switch (btn) {
        case "syncDevices": syncChildren(); break
        case "resetConnection": state.authToken = null; state.connError = null; break
        case "requestApiKey": requestApiKey(); break
        case "btnSelectAllAct": if (state.allActKeys) app.updateSetting("selectedActivities", state.allActKeys); break
        case "btnSelectAllMedia": if (state.allMediaKeys) app.updateSetting("selectedMediaEntities", state.allMediaKeys); break
        case "btnSelectAllHome": if (state.allHomeKeys) app.updateSetting("selectedHomeEntities", state.allHomeKeys); break
        case "btnClearAct": app.updateSetting("selectedActivities", []); break
        case "btnClearMedia": app.updateSetting("selectedMediaEntities", []); break
        case "btnClearHome": app.updateSetting("selectedHomeEntities", []); break
    }
}

def syncChildren() {
    def installedChildren = getChildDevices().collect { it.deviceNetworkId }
    def desiredActivities = settings.selectedActivities ?: []
    def desiredEntities = (settings.selectedMediaEntities ?: []) + (settings.selectedHomeEntities ?: [])
    def orphanWarnings = [] 
    
    // Attempt Global Creation with safety
    try {
        provisionGlobalController(orphanWarnings)
    } catch (e) {
        state.syncMsg = "🛑 Sync Failed: Global Driver Missing."
        return
    }

    def activities = getActivitiesLightweight()
    if (activities == null) {
        pauseExecution(2000)
        activities = getActivitiesLightweight()
    }

    if (activities == null) {
        state.syncMsg = "🛑 Sync Failed: Could not communicate with Remote."
        return
    }
    
    def entities = getEntitiesLightweight()

    int added = 0
    int updated = 0
    
    desiredActivities.each { selectedId ->
        def act = activities.find { 
            def currentId = it.id ?: it.entity_id 
            return selectedId.equals(currentId?.toString())
        }
        if (act) {
            def result = createOrUpdateChild(selectedId, cleanName(act.name), "activity", settings.forceNameSync, orphanWarnings)
            if (result == "added") added++
            if (result == "updated") updated++
        }
    }

    desiredEntities.each { selectedId ->
        def ent = entities.find { 
            def currentId = it.entity_id ?: it.id 
            return selectedId.equals(currentId?.toString())
        }
        if (ent) {
            def result = createOrUpdateChild(selectedId, cleanName(ent.name), ent.entity_type, settings.forceNameSync, orphanWarnings)
            if (result == "added") added++
            if (result == "updated") updated++
        }
    }
    
    getChildDevices().each { child ->
        def dni = child.deviceNetworkId
        if (dni == "UCR3-Global") return 
        boolean isAct = child.typeName == "Remote3ActivityDriver"
        boolean isEnt = child.typeName == "Remote3EntityDriver"
        if ((isAct && !desiredActivities.contains(dni)) || (isEnt && !desiredEntities.contains(dni))) {
            deleteChildDevice(dni)
        }
    }
    
    if (orphanWarnings.size() > 0) {
        state.syncWarnings = orphanWarnings
        if (added == 0) state.syncMsg = "🛑 Sync Failed: No devices created. See errors above."
        else state.syncMsg = "⚠️ Sync Partial: ${added} added, but some devices were blocked."
    } else {
        state.syncWarnings = null
        state.syncMsg = "✅ Sync Complete: ${added} added, ${updated} updated."
    }
    
    debugLog state.syncMsg
}

def createOrUpdateChild(id, label, type, forceUpdate, warningsList) {
    String dni = id.toString()
    def child = getChildDevice(dni)
    
    if (!child) {
        String driverName = (type == "activity") ? "Remote3ActivityDriver" : "Remote3EntityDriver"
        try {
            addChildDevice("pharrison", driverName, dni, null, [name: label, label: label])
            debugLog "SYNC: Created ${label} [${dni}]"
            return "added"
        } catch (Exception e) {
            if (e.message.contains("Driver not found")) {
                if (warningsList != null) warningsList.add("Missing Driver: ${driverName}")
            } else {
                 if (warningsList != null) warningsList.add(label)
            }
        }
        return "skipped"
    } else if (forceUpdate && child.label != label) {
        child.label = label
        debugLog "SYNC: Updated ${label} [${dni}]"
        return "updated"
    }
    return "skipped"
}

def provisionGlobalController(warningsList) {
    String dni = "UCR3-Global"
    String label = "Remote 3 Hub Control"
    def child = getChildDevice(dni)
    if (!child) {
        // This is where the installation often fails if the driver is missing
        addChildDevice("pharrison", "Remote3GlobalDriver", dni, null, [name: label, label: label])
        debugLog "DEVICE CREATED: ${label} (${dni})"
    }
}

def verifyDrivers() {
    // Since we cannot programmatically query "Installed Drivers" without creating a device,
    // we return an empty list here to rely on the try/catch blocks in creation.
    return []
}

// =============================================================================
// SECTION 3: NETWORK
// =============================================================================

def requestApiKey() {
    def auth = "Basic " + "${"web-configurator"}:${settings.remotePin}".bytes.encodeBase64().toString()
    def headers = ["Authorization": auth, "Content-Type": "application/json"]
    try {
        httpGet([uri: "http://${settings.remoteIP}/api/auth/api_keys", headers: headers, ignoreSSLIssues: true, timeout: 3]) { resp ->
            if (resp.data instanceof List) {
                def oldKey = resp.data.find { it.name == "Hubitat" }
                if (oldKey) {
                    try { httpDelete([uri: "http://${settings.remoteIP}/api/auth/api_keys/${oldKey.key_id}", headers: headers, ignoreSSLIssues: true]) { } } catch (e) {}
                    pauseExecution(500)
                }
            }
        }
    } catch (e) {}

    try {
        httpPost([uri: "http://${settings.remoteIP}/api/auth/api_keys", headers: headers, body: '{"name":"Hubitat","scopes":["admin"]}', ignoreSSLIssues: true]) { resp ->
            if (resp.data?.api_key) state.authToken = resp.data.api_key
        }
    } catch (e) { 
        def msg = e.message
        msg = msg.replaceAll(/Connect to .* failed: /, "")
        
        if (msg.contains("422")) {
            state.connError = "PIN may have expired. <b>Please check IP address and PIN.</b>"
        } else if (msg.contains("timed out")) {
            state.connError = "Connection timed out. <b>Please check IP address</b> and ensure Remote is awake."
        } else if (msg.contains("refused")) {
            state.connError = "Connection refused. <b>Please check IP address and PIN.</b>"
        } else if (msg.contains("host")) {
            state.connError = "Host unreachable. <b>Please check IP address.</b>"
        } else {
            state.connError = "Auth Failed: ${msg}. <b>Please check IP address and PIN.</b>"
        }
        
        state.authToken = null
    }
}

def silentVerify() {
    if (!state.authToken || !settings?.remoteIP) return false
    try { 
        def s = false
        httpGet([uri: "http://${settings.remoteIP}/api/activities", headers: ["Authorization": "Bearer ${state.authToken}"], timeout: 3]) { resp -> 
            if (resp.status == 200) s = true 
        }
        return s
    } catch (e) { 
        state.connError = "Verification Failed: ${e.message}. <b>Please check IP address</b> and ensure Remote is awake."
        return false 
    }
}

def getActivitiesLightweight() {
    if (!state.authToken) return null
    try { 
        def r = []
        httpGet([uri: "http://${settings.remoteIP}/api/activities", query: [include_options: false], headers: ["Authorization": "Bearer ${state.authToken}"], timeout: 3]) { resp -> 
            if (resp.data instanceof List) r = resp.data 
        }
        return r
    } catch (e) { return null }
}

def getEntitiesLightweight() {
    if (!state.authToken) return null
    try { 
        def r = []
        httpGet([uri: "http://${settings.remoteIP}/api/entities", query: [limit: 100, include_options: false], headers: ["Authorization": "Bearer ${state.authToken}"], timeout: 3]) { resp -> 
            if (resp.data instanceof List) r = resp.data 
        }
        return r
    } catch (e) { return null }
}

def cleanName(raw) {
    if (raw instanceof Map) return raw.en_US ?: raw.en ?: "Unnamed"
    return raw.toString().replaceAll(/(?i)\(null\)/, "").trim()
}

def getHeaderHTML(status, roundTop = true, roundBottom = true) {
    def color = "#B71C1C" 
    def text = "Disconnected"
    def icon = "🔴"
    
    if (status == "connected") {
        color = "#2E7D32" 
        text = "Connected"
        icon = "🟢"
    } else if (status == "connecting") {
        color = "#E65100"
        text = "Connecting..."
        icon = "🟠"
    }
    
    def rTop = roundTop ? "5px" : "0px"
    def rBot = roundBottom ? "5px" : "0px"
    
    return """
        <div style='background-color: #2E7D32; color: white; padding: 5px 10px; border-top-left-radius: ${rTop}; border-top-right-radius: ${rTop}; border-bottom-left-radius: ${rBot}; border-bottom-right-radius: ${rBot}; display: flex; justify-content: space-between; align-items: flex-end;'>
            <div style='line-height: 1.1;'>
                <div style='font-size: 1.2em; font-weight: bold;'>Unfolded Circle Remote 3</div>
                <div style='font-size: 0.9em;'>By Paul Harrison | v1.0.59</div>
            </div>
            <div style='font-weight: bold; background-color: rgba(0,0,0,0.2); padding: 5px 10px; border-radius: 4px; font-size: 0.9em;'>${icon} ${text}</div>
        </div>
    """
}

def getFooterHTML() {
    // Zero-margin approach to minimize Hubitat's forced paragraph padding
    return """
    <div style='text-align:center; margin: 0; padding: 0;'>
        <hr style='border: 0; height: 1px; background-image: linear-gradient(to right, rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.75), rgba(0, 0, 0, 0)); margin: 3px 0 5px 0;'/>
        <div style='color:#1A77C9; font-size:11px; line-height: 1.1;'>
            Unfolded Circle Remote 3 Integration<br>
            <a href='https://paypal.me/PaulHarrisonDev' target='_blank'>
                <img src='https://www.paypalobjects.com/webstatic/mktg/logo/pp_cc_mark_37x23.jpg' border='0' alt='PayPal Logo' style='margin: 5px 0;'>
            </a><br>
            Please support development.
        </div>
    </div>
    """
}

def debugLog(msg) {
    if (settings.enableDebug != false) log.debug msg
}
