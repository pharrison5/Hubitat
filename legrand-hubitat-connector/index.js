require('dotenv').config();
const { syncDevices } = require('./sync');

console.log('Starting Legrand ↔ Hubitat connector...');
setInterval(syncDevices, parseInt(process.env.SYNC_INTERVAL, 10));
