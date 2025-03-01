const logger = require('./logger')('errorHandler');

// Error categories
const ERROR_CATEGORY = {
  DISCORD_API: 'discord_api',
  COMMAND: 'command',
  CAD_SERVICE: 'cad_service',
  CONFIG: 'config',
  VALIDATION: 'validation',
  UNKNOWN: 'unknown'
};

/**
 * Handle command execution errors
 * @param {Error} error - The error that occurred
 * @param {Object} interaction - Discord interaction object
 * @param {string} commandName - Name of the command that failed
 * @param {Object} context - Additional context data
 */
async function handleCommandError(error, interaction, commandName, context = {}) {
  const errorId = generateErrorId();
  
  logger.error(`Command error in ${commandName} [${errorId}]`, {
    errorId,
    commandName,
    error: {
      message: error.message,
      stack: error.stack,
      code: error.code,
      name: error.name
    },
    ...formatInteractionForLogging(interaction),
    ...context
  });
  
  // If this is an "Unknown interaction" error, just log it and return
  if (error.code === 10062 || error.message.includes('Unknown interaction')) {
    logger.warn(`Interaction ${interaction.id} has expired or was already handled [${errorId}]`);
    return;
  }
  
  // Determine if the error is from the CAD service
  const isCadServiceError = error.message && (
    error.message.includes('ECONNREFUSED') ||
    error.message.includes('CAD service') ||
    error.message.includes('Failed to generate') ||
    error.message.includes('localhost') ||
    error.message.includes('127.0.0.1')
  );
  
  // Craft user-friendly error message
  let errorMessage = `Error: ${error.message}\n\n`;
  
  if (isCadServiceError) {
    errorMessage += "Troubleshooting:\n";
    
    if (error.message.includes('ECONNREFUSED') && error.message.includes('::1')) {
      errorMessage += "• IPv6 connection issue detected. The server is configured to use IPv6 (::1) but can't connect.\n";
      errorMessage += "• Try updating your .env file to set API_BASE_URL=http://127.0.0.1:8000/api to force IPv4.\n";
    } else if (error.message.includes('ECONNREFUSED') && error.message.includes('127.0.0.1')) {
      errorMessage += "• Make sure the FastAPI server is running on port 8000.\n";
      errorMessage += "• Check that there are no firewall rules blocking the connection.\n";
    }
    
    errorMessage += "• Verify that both API and Clojure services are running.";
  } else {
    // For other errors, add the error ID for reference
    errorMessage += `\nError ID: ${errorId}`;
  }
  
  // Try to respond to the interaction
  try {
    if (interaction.replied || interaction.deferred) {
      // Use catch to handle any errors without throwing
      await interaction.editReply({ content: errorMessage }).catch(replyErr => {
        logger.error(`Failed to edit reply for error ${errorId}`, {
          originalError: error.message,
          replyError: replyErr.message,
          interactionId: interaction.id
        });
      });
    } else {
      // Use catch to handle any errors without throwing
      await interaction.reply({ 
        content: errorMessage, 
        ephemeral: true 
      }).catch(replyErr => {
        logger.error(`Failed to reply for error ${errorId}`, {
          originalError: error.message,
          replyError: replyErr.message,
          interactionId: interaction.id
        });
      });
    }
  } catch (replyError) {
    logger.error(`Failed to send error message to user for error ${errorId}`, {
      originalError: error.message,
      replyError: replyError.message,
      interactionId: interaction.id
    });
  }
}

/**
 * Handle startup/initialization errors
 * @param {Error} error - The error that occurred
 * @param {string} component - Name of the component that failed
 * @param {Object} context - Additional context data
 */
function handleStartupError(error, component, context = {}) {
  logger.error(`Startup error in ${component}`, {
    component,
    error: {
      message: error.message,
      stack: error.stack,
      code: error.code,
      name: error.name
    },
    ...context
  });
  
  // For critical startup errors, we might want to exit the process
  if (context.critical) {
    console.error(`Critical startup error in ${component}: ${error.message}`);
    process.exit(1);
  }
}

/**
 * Format interaction data for logging (removing sensitive/circular parts)
 * @param {Object} interaction - Discord interaction
 * @returns {Object} Formatted interaction data for logging
 */
function formatInteractionForLogging(interaction) {
  if (!interaction) return {};
  
  return {
    interactionId: interaction.id,
    channelId: interaction.channelId,
    guildId: interaction.guildId,
    userId: interaction.user?.id,
    options: formatOptionsForLogging(interaction.options)
  };
}

/**
 * Format command options for logging
 * @param {Object} options - Discord command options
 * @returns {Object} Formatted options for logging
 */
function formatOptionsForLogging(options) {
  if (!options || !options.data) return {};
  
  const formattedOptions = {};
  
  try {
    for (const option of options.data) {
      formattedOptions[option.name] = option.value;
    }
  } catch (e) {
    return {};
  }
  
  return formattedOptions;
}

/**
 * Generate a random error ID for tracking
 * @returns {string} Random error ID
 */
function generateErrorId() {
  return Math.random().toString(36).substring(2, 8).toUpperCase();
}

// Export functions and constants
module.exports = {
  ERROR_CATEGORY,
  handleCommandError,
  handleStartupError
};