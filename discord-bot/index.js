const fs = require('fs');
const path = require('path');
const { Client, Collection, Events, GatewayIntentBits } = require('discord.js');
const config = require('./config');
const logger = require('./utils/logger')('main');
const { handleCommandError, handleStartupError } = require('./utils/errorHandler');

// Create a new client instance
const client = new Client({ intents: [GatewayIntentBits.Guilds] });

// Setup process error handlers
process.on('uncaughtException', (error) => {
  logger.error('Uncaught exception', { error: error.stack });
});

process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled rejection', { reason: reason?.stack || reason });
});

// Initialize the bot
async function init() {
  try {
    logger.info('Starting CAD-OS Discord Bot');
    
    // Load commands (now async)
    await loadCommands();
    
    // Set up event handlers
    setupEventHandlers();
    
    // Login to Discord
    logger.info('Logging in to Discord');
    await client.login(config.DISCORD_TOKEN);
    logger.info('Login successful');
  } catch (error) {
    handleStartupError(error, 'bot-initialization', { critical: true });
  }
}

// Load command files
async function loadCommands() {
  try {
    client.commands = new Collection();
    const commandsPath = path.join(__dirname, 'commands');
    const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.js'));
    
    logger.info(`Loading ${commandFiles.length} command files`);
    
    for (const file of commandFiles) {
      try {
        const filePath = path.join(commandsPath, file);
        const command = require(filePath);
        
        if ('data' in command && 'execute' in command) {
          // If this is the dynamic model command, we need to initialize it first
          if (command.getData && typeof command.getData === 'function') {
            try {
              logger.info(`Initializing dynamic command: ${command.data.name}`);
              await command.getData();
            } catch (error) {
              logger.error(`Error initializing dynamic command: ${command.data.name}`, { error: error.stack });
              logger.info('Command will use default data');
            }
          }
          
          logger.debug(`Registering command: ${command.data.name}`);
          client.commands.set(command.data.name, command);
        } else {
          logger.warn(`Command file ${file} is missing required properties`);
        }
      } catch (error) {
        handleStartupError(error, 'command-loading', { file });
      }
    }
  } catch (error) {
    handleStartupError(error, 'commands-loading', { critical: true });
    throw error;
  }
}

// Set up Discord event handlers
function setupEventHandlers() {
  // Ready event
  client.once(Events.ClientReady, (readyClient) => {
    logger.info(`Ready! Logged in as ${readyClient.user.tag}`);
    logger.info(`Bot is in ${readyClient.guilds.cache.size} guild(s)`);
  });
  
  // Interaction event
  client.on(Events.InteractionCreate, async (interaction) => {
    if (!interaction.isChatInputCommand()) return;
    
    const commandName = interaction.commandName;
    logger.info(`Command received: ${commandName}`, {
      userId: interaction.user.id,
      guildId: interaction.guildId,
      channelId: interaction.channelId
    });
    
    const command = client.commands.get(commandName);
    
    if (!command) {
      logger.warn(`No command matching ${commandName} was found`);
      try {
        // Even if we don't have a command, acknowledge the interaction
        await interaction.reply({ 
          content: `Command '${commandName}' not found.`, 
          ephemeral: true 
        });
      } catch (error) {
        logger.error(`Failed to reply to unknown command ${commandName}`, { error: error.message });
      }
      return;
    }
    
    try {
      logger.debug(`Executing command: ${commandName}`);
      // Make sure to wrap the command execution in a try-catch
      await Promise.resolve(command.execute(interaction))
        .catch(error => {
          logger.error(`Error in command execution for ${commandName}`, { error: error.message });
          throw error; // Re-throw to be caught by the outer try-catch
        });
    } catch (error) {
      try {
        await handleCommandError(error, interaction, commandName);
      } catch (handlerError) {
        logger.error(`Error handler failed for ${commandName}`, { 
          originalError: error.message, 
          handlerError: handlerError.message 
        });
      }
    }
  });
}

// Start the bot and keep the process running
(async () => {
  try {
    await init();
    logger.info('Bot initialization complete');
  } catch (error) {
    logger.error('Failed to initialize bot', { error: error.stack });
    process.exit(1);
  }
})();