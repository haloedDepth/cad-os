const fs = require('fs');
const path = require('path');
const { Client, Collection, Events, GatewayIntentBits } = require('discord.js');
const config = require('./config');

// Create a new client instance
const client = new Client({ intents: [GatewayIntentBits.Guilds] });

// Load commands
client.commands = new Collection();
const commandsPath = path.join(__dirname, 'commands');
const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.js'));

console.log('Loading commands...');
for (const file of commandFiles) {
  const filePath = path.join(commandsPath, file);
  const command = require(filePath);
  
  if ('data' in command && 'execute' in command) {
    console.log(`Registering command: ${command.data.name}`);
    client.commands.set(command.data.name, command);
  } else {
    console.log(`[WARNING] The command at ${filePath} is missing a required "data" or "execute" property.`);
  }
}

// Event handlers
client.once(Events.ClientReady, readyClient => {
  console.log(`Ready! Logged in as ${readyClient.user.tag}`);
  console.log(`Bot is in ${readyClient.guilds.cache.size} guild(s)`);
});

client.on(Events.InteractionCreate, async interaction => {
  if (!interaction.isChatInputCommand()) return;

  const command = interaction.client.commands.get(interaction.commandName);

  if (!command) {
    console.error(`No command matching ${interaction.commandName} was found.`);
    return;
  }

  try {
    console.log(`Executing command: ${interaction.commandName}`);
    await command.execute(interaction);
  } catch (error) {
    console.error(`Error executing command: ${interaction.commandName}`, error);
    
    // Check if the error is an Unknown Interaction
    if (error.code === 10062) {
      console.log('Interaction timed out or was already acknowledged.');
      return; // Don't try to respond to an unknown interaction
    }
    
    const errorMessage = 'There was an error while executing this command!';
    
    try {
      if (interaction.replied || interaction.deferred) {
        await interaction.followUp({ content: errorMessage, ephemeral: true });
      } else {
        await interaction.reply({ content: errorMessage, ephemeral: true });
      }
    } catch (replyError) {
      console.error('Error sending error response:', replyError);
      // No need to do anything else here - we're just preventing a crash
    }
  }
});

// Login to Discord with your client's token
client.login(config.DISCORD_TOKEN)
  .then(() => console.log('Bot logged in successfully!'))
  .catch(error => console.error('Failed to log in:', error));