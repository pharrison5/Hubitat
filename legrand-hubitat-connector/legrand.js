const axios = require('axios');
require('dotenv').config();

async function authenticateLegrand() {
  try {
    const response = await axios.post(`${process.env.LEGRAND_BASE_URL}/login`, {
      username: process.env.LEGRAND_USER,
      password: process.env.LEGRAND_PASS
    });
    return response.data.token;
  } catch (error) {
    console.error('Legrand authentication failed:', error.message);
    throw error;
  }
}

async function getLegrandDevices(token) {
  try {
    const response = await axios.get(`${process.env.LEGRAND_BASE_URL}/devices`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  } catch (error) {
    console.error('Failed to fetch Legrand devices:', error.message);
    throw error;
  }
}

module.exports = { authenticateLegrand, getLegrandDevices };
