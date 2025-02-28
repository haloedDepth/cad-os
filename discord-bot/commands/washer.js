const { SlashCommandBuilder, AttachmentBuilder } = require('discord.js');
const cadService = require('../services/cad-service');
const logger = require('../utils/logger')('washer-command');
const { handleCommandError } = require('../utils/errorHandler');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('washer')
    .setDescription('Generate a washer model with specified parameters')
    .addNumberOption(option => 
      option.setName('outer_diameter')
        .setDescription('Outer diameter of the washer')
        .setRequired(true)
        .setMinValue(0.1))
    .addNumberOption(option => 
      option.setName('inner_diameter')
        .setDescription('Inner diameter of the washer')
        .setRequired(true)
        .setMinValue(0.1))
    .addNumberOption(option => 
      option.setName('thickness')
        .setDescription('Thickness of the washer')
        .setRequired(true)
        .setMinValue(0.1)),
  
  async execute(interaction) {
    try {
      logger.info('Washer command execution started', {
        userId: interaction.user.id,
        guildId: interaction.guildId
      });
      
      // Immediately defer the reply to prevent timeout
      await interaction.deferReply();
      logger.debug('Reply deferred');
      
      const outerDiameter = interaction.options.getNumber('outer_diameter');
      const innerDiameter = interaction.options.getNumber('inner_diameter');
      const thickness = interaction.options.getNumber('thickness');
      
      logger.info('Washer parameters received', { 
        outerDiameter, 
        innerDiameter, 
        thickness 
      });
      
      // Validate parameters
      if (innerDiameter >= outerDiameter) {
        logger.warn('Invalid parameters: inner diameter >= outer diameter', {
          innerDiameter,
          outerDiameter
        });
        
        return interaction.editReply({
          content: 'Error: Inner diameter must be less than outer diameter.'
        });
      }
      
      // Create parameters object
      const params = {
        outer_diameter: outerDiameter,
        inner_diameter: innerDiameter,
        thickness: thickness
      };
      
      await interaction.editReply(
        `Generating washer model with:\nOuter Diameter: ${outerDiameter}\nInner Diameter: ${innerDiameter}\nThickness: ${thickness}`
      );
      
      try {
        // Generate the model
        const result = await cadService.generateModel('washer', params);
        logger.info('Model generation successful', { result });
        
        // Get the filename
        const fileName = result.fileName;
        
        // Try to render the model and get an image
        let imagePath = null;
        try {
          logger.info('Attempting to render the model', { fileName });
          imagePath = await cadService.renderModel(fileName, 'washer');
          logger.info('Rendering successful', { imagePath });
          
          // Send the rendered image
          const attachment = new AttachmentBuilder(imagePath, { name: 'washer_render.png' });
          
          await interaction.editReply({
            content: `Washer model generated successfully!\n` +
                    `Parameters: Outer Diameter: ${outerDiameter}, Inner Diameter: ${innerDiameter}, Thickness: ${thickness}`,
            files: [attachment]
          });
          
          logger.info('Sent message with rendered image');
        } catch (renderError) {
          logger.warn('Rendering failed, sending text-only response', { 
            error: renderError.message 
          });
          
          // Just send model info if rendering failed
          await interaction.editReply({
            content: `Washer model generated successfully!\n` +
                    `Parameters: Outer Diameter: ${outerDiameter}, Inner Diameter: ${innerDiameter}, Thickness: ${thickness}\n` +
                    `Model file: ${fileName}\n\n` +
                    `(Failed to render model image: ${renderError.message})`
          });
        }
      } catch (error) {
        throw error;
      }
    } catch (error) {
      await handleCommandError(error, interaction, 'washer', {
        params: {
          outerDiameter: interaction.options.getNumber('outer_diameter'),
          innerDiameter: interaction.options.getNumber('inner_diameter'),
          thickness: interaction.options.getNumber('thickness')
        }
      });
    }
  },
};