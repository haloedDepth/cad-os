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
    try {
      logger.info('Model command execution started', {
        userId: interaction.user.id,
        guildId: interaction.guildId
      });
      
      // Immediately defer the reply to prevent timeout
      await interaction.deferReply();
      logger.debug('Reply deferred');
      
      // Get selected model type from subcommand
      const modelType = interaction.options.getSubcommand();
      logger.info(`Selected model type: ${modelType}`);
      
      // Get schema for the selected model type
      const schema = await cadService.getModelSchema(modelType);
      logger.info(`Retrieved schema for ${modelType}`, { 
        paramCount: schema.parameters?.length || 0 
      });
      
      // Extract parameters from interaction based on schema
      const params = {};
      
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
        return interaction.editReply(errorMessage);
      }
      
      // Format parameter list for the reply message
      const paramList = validator.formatParameterList(params, schema);
      
      await interaction.editReply(
        `Generating ${modelType} model with parameters:\n${paramList}`
      );
      
      try {
        // Generate the model
        const result = await cadService.generateModel(modelType, params);
        logger.info('Model generation successful', { result });
        
        // Get the filename
        const fileName = result.fileName;
        
        // Try to render the model and get an image
        try {
          logger.info('Attempting to render the model', { fileName });
          const imagePath = await cadService.renderModel(fileName, modelType);
          logger.info('Rendering successful', { imagePath });
          
          // Send the rendered image
          const attachment = new AttachmentBuilder(imagePath, { name: `${modelType}_render.png` });
          
          await interaction.editReply({
            content: `${modelType} model generated successfully!\n` +
                    `Parameters:\n${paramList}`,
            files: [attachment]
          });
          
          logger.info('Sent message with rendered image');
        } catch (renderError) {
          logger.warn('Rendering failed, sending text-only response', { 
            error: renderError.message 
          });
          
          // Just send model info if rendering failed
          await interaction.editReply({
            content: `${modelType} model generated successfully!\n` +
                    `Parameters:\n${paramList}\n` +
                    `Model file: ${fileName}\n\n` +
                    `(Failed to render model image: ${renderError.message})`
          });
        }
      } catch (error) {
        throw error;
      }
    } catch (error) {
      await handleCommandError(error, interaction, 'model', {
        modelType: interaction.options.getSubcommand(),
        params
      });
    }
  },
};