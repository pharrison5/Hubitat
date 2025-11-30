const { authenticateLegrand, getLegrandDevices } = require('./legrand');
const { sendToHubitat } = require('./hubitat');

async function syncDevices() {
  try {
    const token = await authenticateLegrand();
    const devices = await getLegrandDevices(token);

    for (const device of devices) {
      if (device.type === 'light') {
        const hubitatCommand = device.state === 'on' ? 'on' : 'off';
        if (device.hubitatId) {
          await sendToHubitat(device.hubitatId, hubitatCommand);
        } else {
          console.warn(`No Hubitat ID mapped for Legrand device ${device.name}`);
        }
      }
    }
  } catch (error) {
    console.error('Error during sync:', error.message);
  }
}

module.exports = { syncDevices };
