const { authenticateLegrand, getLegrandDevices } = require('./legrand');
const { sendToHubitat } = require('./hubitat');
const { discoverLegrandHub } = require('./discovery');

async function syncDevices() {
  const baseUrl = await discoverLegrandHub();
  if (!baseUrl) return; // Exit if no hub found

  try {
    const token = await authenticateLegrand(baseUrl);
    const devices = await getLegrandDevices(baseUrl, token);

    for (const device of devices) {
      if (device.type === 'light' && device.hubitatId) {
        const hubitatCommand = device.state === 'on' ? 'on' : 'off';
        await sendToHubitat(device.hubitatId, hubitatCommand);
      }
    }
  } catch (error) {
    console.error('Error during sync:', error.message);
  }
}

module.exports = { syncDevices };
