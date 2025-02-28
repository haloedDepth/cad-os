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
    
    // Load commands
    client.commands = new Collection();
    loadCommands();
    
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
function loadCommands() {
  const commandsPath = path.join(__dirname, 'commands');
  const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.js'));
  
  logger.info(`Loading ${commandFiles.length} command files`);
  
  for (const file of commandFiles) {
    try {
      const filePath = path.join(commandsPath, file);
      const command = require(filePath);
      
      if ('data' in command && 'execute' in command) {
        logger.debug(`Registering command: ${command.data.name}`);
        client.commands.set(command.data.name, command);
      } else {
        logger.warn(`Command file ${file} is missing required properties`);
      }
    } catch (error) {
      handleStartupError(error, 'command-loading', { file });
    }
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
      return;
    }
    
    try {
      logger.debug(`Executing command: ${commandName}`);
      await command.execute(interaction);
    } catch (error) {
      await handleCommandError(error, interaction, commandName);
    }
  });
}

// Start the bot
init();