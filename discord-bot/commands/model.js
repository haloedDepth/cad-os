const { SlashCommandBuilder, AttachmentBuilder } = require('discord.js');
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
          const imagePath = await cadService.renderModel(fileName, modelType);
          logger.info('Rendering successful', { imagePath });
          
          // Send the rendered image
          const attachment = new AttachmentBuilder(imagePath, { name: `${modelType}_render.png` });
          
          if (isInteractionDeferred) {
            await interaction.editReply({
              content: `${modelType} model generated successfully!\n` +
                      `Parameters:\n${paramList}`,
              files: [attachment]
            });
          }
          
          logger.info('Sent message with rendered image');
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
};