const axios = require('axios');

async function authenticateLegrand(baseUrl) {
  const response = await axios.post(`${baseUrl}/login`, {
    username: 'your_username', // Replace with discovery or prompt logic
    password: 'your_password'
  });
  return response.data.token;
}

async function getLegrandDevices(baseUrl, token) {
  const response = await axios.get(`${baseUrl}/devices`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  return response.data;
}

module.exports = { authenticateLegrand, getLegrandDevices };
