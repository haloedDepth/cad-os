const { SlashCommandBuilder, AttachmentBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const cadService = require('../services/cad-service');
const logger = require('../utils/logger')('model-command');
const { handleCommandError } = require('../utils/errorHandler');
const validator = require('../utils/validator');

// This will be built dynamically from the available models
let commandBuilder = null;

// Function to initialize the command - called when the bot starts
async function initializeCommand() {
  try {
    logger.info('Initializing dynamic model command');
    
    // Fetch available model types
    const modelTypes = await cadService.getModelTypes();
    logger.info(`Retrieved ${modelTypes.length} model types`, { modelTypes });
    
    // Create base command builder
    const builder = new SlashCommandBuilder()
      .setName('model')
      .setDescription('Generate different 3D models');
    
    // For each model type, add a subcommand with its own parameters
    for (const modelType of modelTypes) {
      try {
        const schema = await cadService.getModelSchema(modelType);
        logger.info(`Retrieved schema for ${modelType}`, { 
          paramCount: schema.parameters?.length || 0 
        });
        
        // Create a subcommand for this model type
        builder.addSubcommand(subcommand => {
          subcommand
            .setName(modelType)
            .setDescription(`Generate a ${modelType} model`);
          
          // Add parameters specific to this model type
          if (schema.parameters && Array.isArray(schema.parameters)) {
            schema.parameters.forEach(param => {
              // Skip hidden parameters
              if (param.hidden) return;
              
              // Format parameter name - Discord doesn't allow hyphens in param names
              const paramName = param.name.replace(/-/g, '_');
              
              // For number parameters
              if (param.type === 'number') {
                subcommand.addNumberOption(option => {
                  const opt = option.setName(paramName)
                    .setDescription(param.description || `${param.name} for ${modelType}`)
                    .setRequired(!param.default);
                  
                  // Add min value for common CAD parameters
                  if (param.default !== undefined) {
                    opt.setMinValue(0.1);
                  }
                  
                  return opt;
                });
              } 
              // For other parameter types
              else {
                subcommand.addStringOption(option => 
                  option.setName(paramName)
                    .setDescription(param.description || `${param.name} for ${modelType}`)
                    .setRequired(!param.default)
                );
              }
            });
          }
          
          return subcommand;
        });
      } catch (error) {
        logger.error(`Failed to load schema for ${modelType}`, { error: error.message });
      }
    }
    
    // Store the built command
    commandBuilder = builder;
    logger.info('Dynamic model command initialized successfully');
    
    return builder;
  } catch (error) {
    logger.error('Failed to initialize dynamic model command', { error: error.stack });
    throw error;
  }
}

// Views available for navigation
const VIEWS = {
  FRONT: 'front',
  RIGHT: 'right',
  BACK: 'back',
  LEFT: 'left',
  TOP: 'top'
};

// Create navigation buttons for the model viewer
function createViewButtons(fileName, modelType, currentView = VIEWS.FRONT) {
  const row = new ActionRowBuilder();
  
  // Left button
  row.addComponents(
    new ButtonBuilder()
      .setCustomId(`view_${VIEWS.LEFT}_${fileName}_${modelType}`)
      .setLabel('◀️ Left')
      .setStyle(currentView === VIEWS.LEFT ? ButtonStyle.Primary : ButtonStyle.Secondary)
  );
  
  // Front button
  row.addComponents(
    new ButtonBuilder()
      .setCustomId(`view_${VIEWS.FRONT}_${fileName}_${modelType}`)
      .setLabel('⬆️ Front')
      .setStyle(currentView === VIEWS.FRONT ? ButtonStyle.Primary : ButtonStyle.Secondary)
  );
  
  // Right button
  row.addComponents(
    new ButtonBuilder()
      .setCustomId(`view_${VIEWS.RIGHT}_${fileName}_${modelType}`)
      .setLabel('▶️ Right')
      .setStyle(currentView === VIEWS.RIGHT ? ButtonStyle.Primary : ButtonStyle.Secondary)
  );
  
  // Back button
  row.addComponents(
    new ButtonBuilder()
      .setCustomId(`view_${VIEWS.BACK}_${fileName}_${modelType}`)
      .setLabel('⬇️ Back')
      .setStyle(currentView === VIEWS.BACK ? ButtonStyle.Primary : ButtonStyle.Secondary)
  );
  
  // Add a second row for top view
  const row2 = new ActionRowBuilder();
  row2.addComponents(
    new ButtonBuilder()
      .setCustomId(`view_${VIEWS.TOP}_${fileName}_${modelType}`)
      .setLabel('🔝 Top')
      .setStyle(currentView === VIEWS.TOP ? ButtonStyle.Primary : ButtonStyle.Secondary)
  );
  
  return [row, row2];
}

module.exports = {
  // This ensures the command is initialized before being used
  async getData() {
    if (!commandBuilder) {
      await initializeCommand();
    }
    return commandBuilder;
  },
  
  data: new SlashCommandBuilder()
    .setName('model')
    .setDescription('Generate 3D models (loading available types...)'),
  
  async execute(interaction) {
    // Initialize params and modelType at the beginning to avoid reference errors
    let params = {};
    let modelType = '';
    let isInteractionDeferred = false;
    
    try {
      // CRITICAL: DEFER THE INTERACTION IMMEDIATELY - no processing before this!
      // This is step #1 - nothing should happen before this
      try {
        logger.debug('Attempting to defer reply');
        // This tells Discord we're working on it and will respond later
        await interaction.deferReply();
        isInteractionDeferred = true;
        logger.info('Interaction deferred successfully');
      } catch (deferError) {
        logger.error('Failed to defer interaction', { error: deferError.message });
        return; // Can't continue if we couldn't secure the interaction
      }
      
      // Now log that we're starting (after deferring)
      logger.info('Model command execution started', {
        userId: interaction.user.id,
        guildId: interaction.guildId
      });
      
      // Get selected model type from subcommand
      modelType = interaction.options.getSubcommand();
      logger.info(`Selected model type: ${modelType}`);
      
      // Get schema for the selected model type
      logger.debug(`Fetching schema for: ${modelType}`);
      const schema = await cadService.getModelSchema(modelType);
      logger.info(`Retrieved schema for ${modelType}`, { 
        paramCount: schema.parameters?.length || 0 
      });
      
      // Extract parameters from interaction based on schema
      if (schema.parameters && Array.isArray(schema.parameters)) {
        schema.parameters.forEach(param => {
          // Skip hidden parameters
          if (param.hidden) return;
          
          // Discord doesn't allow hyphens in parameter names,
          // so we converted them to underscores in the command definition
          const discordParamName = param.name.replace(/-/g, '_');
          
          // Get the parameter value based on its type
          let value;
          if (param.type === 'number') {
            value = interaction.options.getNumber(discordParamName);
          } else {
            value = interaction.options.getString(discordParamName);
          }
          
          // If value is not null or undefined, add it to params
          if (value !== null && value !== undefined) {
            params[param.name] = value;
          } 
          // Otherwise, use default value if available
          else if (param.default !== undefined) {
            params[param.name] = param.default;
          }
        });
      }
      
      logger.info(`Parameters for ${modelType}`, { params });
      
      // Validate parameters against schema
      const validation = validator.validateParameters(params, schema);
      
      if (!validation.valid) {
        const errorMessage = `Validation errors:\n${validation.errors.join('\n')}`;
        logger.warn('Parameter validation failed', { errors: validation.errors });
        if (isInteractionDeferred) {
          return interaction.editReply(errorMessage);
        }
        return;
      }
      
      // Format parameter list for the reply message
      const paramList = validator.formatParameterList(params, schema);
      
      if (isInteractionDeferred) {
        await interaction.editReply(
          `Generating ${modelType} model with parameters:\n${paramList}`
        );
      }
      
      try {
        // Generate the model
        logger.debug('Starting model generation');
        const result = await cadService.generateModel(modelType, params);
        logger.info('Model generation successful', { result });
        
        // Get the filename
        const fileName = result.fileName;
        
        // Update the message to show progress
        if (isInteractionDeferred) {
          await interaction.editReply(
            `${modelType} model generated successfully! Preparing rendering...`
          );
        }
        
        // Try to render the model and get an image
        try {
          logger.info('Attempting to render the model', { fileName });
          const imagePath = await cadService.renderModel(fileName, modelType, VIEWS.FRONT);
          logger.info('Rendering successful', { imagePath });
          
          // Create view navigation buttons
          const buttons = createViewButtons(fileName, modelType, VIEWS.FRONT);
          
          // Send the rendered image with navigation buttons
          const attachment = new AttachmentBuilder(imagePath, { name: `${modelType}_${VIEWS.FRONT}.png` });
          
          if (isInteractionDeferred) {
            await interaction.editReply({
              content: `${modelType} model generated successfully!\n` +
                      `Parameters:\n${paramList}`,
              files: [attachment],
              components: buttons
            });
          }
          
          logger.info('Sent message with rendered image and navigation buttons');
        } catch (renderError) {
          logger.warn('Rendering failed, sending text-only response', { 
            error: renderError.message 
          });
          
          // Just send model info if rendering failed
          if (isInteractionDeferred) {
            await interaction.editReply({
              content: `${modelType} model generated successfully!\n` +
                      `Parameters:\n${paramList}\n` +
                      `Model file: ${fileName}\n\n` +
                      `(Failed to render model image: ${renderError.message})`
            });
          }
        }
      } catch (error) {
        throw error;
      }
    } catch (error) {
      try {
        // Only try to handle command error if we have successfully deferred the interaction
        if (isInteractionDeferred) {
          await handleCommandError(error, interaction, 'model', {
            modelType,
            params
          });
        } else {
          // Just log the error if we can't respond to the interaction
          logger.error(`Error in model command (interaction not deferred)`, {
            error: error.message,
            modelType,
            params
          });
        }
      } catch (handlerError) {
        // If the error handler itself fails, just log it
        logger.error('Error handler failed', { 
          originalError: error.message, 
          handlerError: handlerError.message 
        });
      }
    }
  },
  
  // Handle button interactions for view navigation
  async handleViewButtonInteraction(interaction) {
    const customId = interaction.customId;
    logger.info(`View button clicked: ${customId}`);
    
    // Parse the custom ID to get the view type, filename, and model type
    // Format: view_<view>_<filename>_<modelType>
    const parts = customId.split('_');
    
    if (parts.length < 4) {
      logger.error(`Invalid button custom ID: ${customId}`);
      await interaction.reply({ content: 'Invalid button interaction', ephemeral: true });
      return;
    }
    
    const viewType = parts[1];
    const fileName = parts[2];
    // Model type could contain underscores, so we join all remaining parts
    const modelType = parts.slice(3).join('_');
    
    logger.info(`Changing view to ${viewType} for ${fileName}`, { modelType });
    
    try {
      // Defer the update to avoid interaction timeout
      await interaction.deferUpdate();
      
      // Render the requested view
      const imagePath = await cadService.renderModel(fileName, modelType, viewType);
      
      // Create updated buttons with the new selected view
      const buttons = createViewButtons(fileName, modelType, viewType);
      
      // Get the original message content
      const originalMessage = interaction.message;
      const content = originalMessage.content;
      
      // Create a new attachment with the new view
      const attachment = new AttachmentBuilder(imagePath, { name: `${modelType}_${viewType}.png` });
      
      // Update the message with the new view
      await interaction.editReply({
        content: content,
        files: [attachment],
        components: buttons
      });
      
      logger.info(`View changed to ${viewType} successfully`);
    } catch (error) {
      logger.error(`Error changing view: ${error.message}`, { error: error.stack });
      
      // Try to notify the user about the error without disrupting the UI
      try {
        await interaction.followUp({
          content: `Error changing view: ${error.message}`,
          ephemeral: true
        });
      } catch (followUpError) {
        logger.error(`Failed to send error message: ${followUpError.message}`);
      }
    }
  }
};