#!/usr/bin/env node

const fetch = require('node-fetch');

// Replace these with your actual endpoints/tokens for testing
const HUBITAT_URL = process.env.HUBITAT_MAKER_API_URL || 'http://<hubitat-ip>/apps/api/<app-id>/devices';
const HUBITAT_TOKEN = process.env.HUBITAT_ACCESS_TOKEN || '<your-hubitat-token>';
const LEGRAND_AUTH_URL = 'https://api.developer.legrand.com/oauth2/token'; // Legrand OAuth endpoint

async function checkHubitat() {
    console.log('Checking Hubitat connectivity...');
    try {
        const res = await fetch(`${HUBITAT_URL}?access_token=${HUBITAT_TOKEN}`);
        if (res.ok) {
            console.log('✅ Hubitat Maker API reachable.');
        } else {
            console.error(`❌ Hubitat API error: ${res.status} ${res.statusText}`);
        }
    } catch (err) {
        console.error('❌ Hubitat connectivity failed:', err.message);
    }
}

async function checkLegrand() {
    console.log('Checking Legrand API connectivity...');
    try {
        const res = await fetch(LEGRAND_AUTH_URL, { method: 'OPTIONS' }); // lightweight check
        if (res.ok) {
            console.log('✅ Legrand API reachable.');
        } else {
            console.error(`❌ Legrand API error: ${res.status} ${res.statusText}`);
        }
    } catch (err) {
        console.error('❌ Legrand connectivity failed:', err.message);
    }
}

(async () => {
    await checkHubitat();
    await checkLegrand();
})();
