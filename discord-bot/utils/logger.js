const fs = require('fs');
const path = require('path');
const { createLogger, format, transports } = require('winston');
const { combine, timestamp, printf, colorize, splat } = format;

// Ensure logs directory exists
const logDir = path.join(__dirname, '..', 'logs');
if (!fs.existsSync(logDir)) {
  fs.mkdirSync(logDir, { recursive: true });
}

// Custom log formatter
const customFormat = printf(({ level, message, timestamp, ...rest }) => {
  let logMessage = `${timestamp} [${level}]: ${message}`;
  if (Object.keys(rest).length > 0) {
    // Exclude circular references and non-serializable values
    const context = JSON.stringify(rest, (key, value) => {
      if (key === 'client' || key === 'socket' || key === 'tls') {
        return '[circular]';
      }
      return value;
    });
    logMessage += ` ${context}`;
  }
  return logMessage;
});

// Create the logger
const logger = createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: combine(
    timestamp(),
    splat(),
    customFormat
  ),
  transports: [
    // Write logs to console
    new transports.Console({
      format: combine(
        colorize(),
        timestamp(),
        splat(),
        customFormat
      )
    }),
    // Write logs to file
    new transports.File({ 
      filename: path.join(logDir, 'error.log'), 
      level: 'error' 
    }),
    new transports.File({ 
      filename: path.join(logDir, 'combined.log') 
    })
  ]
});

// Add daily rotation file transport if needed
if (process.env.NODE_ENV === 'production') {
  logger.add(new transports.File({
    filename: path.join(logDir, `${new Date().toISOString().split('T')[0]}.log`)
  }));
}

// Export a wrapper to add module context
module.exports = function(module) {
  const moduleName = module || 'app';
  
  return {
    debug: (message, meta = {}) => {
      logger.debug(message, { module: moduleName, ...meta });
    },
    info: (message, meta = {}) => {
      logger.info(message, { module: moduleName, ...meta });
    },
    warn: (message, meta = {}) => {
      logger.warn(message, { module: moduleName, ...meta });
    },
    error: (message, meta = {}) => {
      logger.error(message, { module: moduleName, ...meta });
    }
  };
};

// Direct export for simpler usage
module.exports.raw = logger;