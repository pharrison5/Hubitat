# Unfolded Circle Remote 3 Integration for Hubitat

An unofficial, stable Hubitat Elevation integration for controlling the Unfolded Circle Remote 3 via its local API. This integration allows Hubitat to control Activities and individual Entities (lights, media players, receivers) and receive status updates via polling.

---

## ✨ Features

* **Activity Control:** Control activities (e.g., "Watch TV") with dedicated Switch capabilities (`on`/`off`).
* **Entity Control:** Control individual devices with standard capabilities (`AudioVolume`, `MediaTransport`, `SwitchLevel`).
* **Universal Command (`sendKey`):** Access all entity-specific and non-standard commands (e.g., `input_hdmi1`, `launch_netflix`) using the flexible `sendKey("command_name")` custom action.
* **Stability:** Uses aggressive connection polling to reliably wake the remote and ensure commands are executed, bypassing the Remote 3's aggressive sleep mode.
* **HPM Ready:** Fully compatible with Hubitat Package Manager for easy installation and updates.

## 💾 Installation

### Option 1: Hubitat Package Manager (Recommended)

1.  Open the **Hubitat Package Manager (HPM)** app.
2.  Select **Install**.
3.  Choose **Search by Tags** or **Search by Name** and search for "Unfolded Circle".
4.  Select the **"Unfolded Circle Remote 3 Integration"** and install all components (1 App and 3 Drivers).

### Option 2: Manual Installation

1.  Go to **Drivers Code** in your Hubitat UI. Create three new drivers and paste the code from the respective `.groovy` files.
2.  Go to **Apps Code**. Create one new app and paste the code from `UnfoldedCircleRemote3App.groovy`.

---

## ⚙️ Configuration

1.  Go to the **Apps** section and select **Add User App**.
2.  Choose the **Unfolded Circle Remote 3 App**.
3.  Follow the on-screen instructions to:
    * Enter the **IP Address** of your Unfolded Circle Remote 3.
    * Enter your **API Authentication Token**.
    * Select the **Activities** and **Entities** you wish to expose to Hubitat.
4.  The App will create the following devices:
    * **Global Hub Device** (using `UC Remote 3 Global Driver`)
    * **Activity Devices** (using `UC Remote 3 Activity Driver`)
    * **Entity Devices** (using `UC Remote 3 Entity Driver`)

---

## 💡 Usage and Advanced Commands

### Activity Control (e.g., "Watch TV" Device)

* **Turn On:** Sends the API command `start`.
* **Turn Off:** Sends the required API command `off` to terminate the activity.
* **Refresh:** Polls the remote to check the current activity state.

### Entity Control (e.g., Marantz Receiver Device)

The most unique and powerful feature is the **`sendKey`** command, which allows you to access all specific commands exposed by the remote for any device.

1.  **Find the Command Name:** You must look up the exact command verb required by the remote for a specific function (e.g., `input_hdmi2`, `set_app_spotify`, `toggle_power`).
2.  **Use in Rule Machine:** In Rule Machine, select **Run Custom Action**, choose the **`sendKey`** command, and enter the command verb as a string parameter.

This method ensures you have full control over all thousands of device-specific commands without manual driver updates.

---

## ⚖️ License

This project is licensed under the **MIT License**.

---
*Created by **Paul Harrison** (2025)*
