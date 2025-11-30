const axios = require('axios');
require('dotenv').config();

async function sendToHubitat(deviceId, command) {
  try {
    const url = `${process.env.HUBITAT_BASE_URL}/devices/${deviceId}/${command}?access_token=${process.env.HUBITAT_TOKEN}`;
    await axios.get(url);
    console.log(`Sent command "${command}" to Hubitat device ${deviceId}`);
  } catch (error) {
    console.error(`Failed to send command to Hubitat device ${deviceId}:`, error.message);
  }
}

