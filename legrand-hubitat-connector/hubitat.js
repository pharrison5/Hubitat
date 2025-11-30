const axios = require('axios');

// If Hubitat supports discovery, add similar logic here
const HUBITAT_BASE_URL = 'http://hubitat.local/apps/api/your_app_id';
const HUBITAT_TOKEN = 'your_hubitat_token';

async function sendToHubitat(deviceId, command) {
  const url = `${HUBITAT_BASE_URL}/devices/${deviceId}/${command}?access_token=${HUBITAT_TOKEN}`;
  await axios.get(url);
  console.log(`Sent command "${command}" to Hubitat device ${deviceId}`);
}

module.exports = { sendToHubitat };
