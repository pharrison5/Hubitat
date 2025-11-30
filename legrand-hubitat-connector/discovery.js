const mdns = require('mdns');

function discoverLegrandHub(timeout = 5000) {
  return new Promise((resolve) => {
    const browser = mdns.createBrowser(mdns.tcp('http'));
    let foundHub = null;

    browser.on('serviceUp', (service) => {
      if (service.name.toLowerCase().includes('legrand') || service.host.toLowerCase().includes('legrand')) {
        console.log('Discovered Legrand Hub:', service.addresses[0]);
        foundHub = `http://${service.addresses[0]}`;
      }
    });

    browser.start();

    setTimeout(() => {
      browser.stop();
      if (foundHub) {
        resolve(foundHub);
      } else {
        console.error('No Legrand hub discovered. Exiting.');
        resolve(null);
      }
    }, timeout);
  });
}

module.exports = { discoverLegrandHub };
