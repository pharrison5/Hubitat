FROM node:18-alpine

# Set working directory
WORKDIR /app

# Copy files
COPY . .

# Install dependencies
RUN npm install

# Install PM2 globally (optional for process management)
RUN npm install -g pm2

# Run health check before starting the app
CMD ["sh", "-c", "node health-check.js && pm2-runtime index.js"]
