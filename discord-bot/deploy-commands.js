const { REST, Routes } = require('discord.js');
const fs = require('fs');
const path = require('path');
const config = require('./config');

async function loadCommands() {
  const commands = [];
  const commandsPath = path.join(__dirname, 'commands');
  const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.js'));

  console.log('Loading command files...');
  for (const file of commandFiles) {
    const filePath = path.join(commandsPath, file);
    const command = require(filePath);
    
    if ('data' in command && 'execute' in command) {
      console.log(`Loading command: ${command.data.name}`);
      
      // If this is the dynamic model command, we need to initialize it first
      if (command.getData && typeof command.getData === 'function') {
        try {
          console.log(`Initializing dynamic command: ${command.data.name}`);
          const builder = await command.getData();
          commands.push(builder.toJSON());
        } catch (error) {
          console.error(`Error initializing dynamic command: ${command.data.name}`, error);
          console.log('Using default command data as fallback');
          commands.push(command.data.toJSON());
        }
      } else {
        commands.push(command.data.toJSON());
      }
    } else {
      console.log(`[WARNING] The command at ${filePath} is missing a required "data" or "execute" property.`);
    }
  }
  
  return commands;
}

(async () => {
  try {
    const commands = await loadCommands();
    
    console.log(`Started refreshing ${commands.length} application (/) commands.`);

    const rest = new REST().setToken(config.DISCORD_TOKEN);
    
    let data;
    
    if (config.GUILD_ID) {
      // Guild-specific deployment (faster for development)
      console.log(`Deploying to guild ${config.GUILD_ID}...`);
      data = await rest.put(
        Routes.applicationGuildCommands(config.CLIENT_ID, config.GUILD_ID),
        { body: commands },
      );
    } else {
      // Global deployment (can take up to an hour to propagate)
      console.log('Deploying globally (this can take up to an hour to propagate)...');
      data = await rest.put(
        Routes.applicationCommands(config.CLIENT_ID),
        { body: commands },
      );
    }

    console.log(`Successfully reloaded ${data.length} application (/) commands.`);
  } catch (error) {
    console.error(error);
  }
})();