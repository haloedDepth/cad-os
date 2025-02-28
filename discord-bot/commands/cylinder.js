const { SlashCommandBuilder, AttachmentBuilder } = require('discord.js');
const cadService = require('../services/cad-service');
const logger = require('../utils/logger')('cylinder-command');
const { handleCommandError } = require('../utils/errorHandler');

module.exports = {
  data: new SlashCommandBuilder()
    .setName('cylinder')
    .setDescription('Generate a cylinder model with specified parameters')
    .addNumberOption(option => 
      option.setName('radius')
        .setDescription('Radius of the cylinder')
        .setRequired(true)
        .setMinValue(0.1))
    .addNumberOption(option => 
      option.setName('height')
        .setDescription('Height of the cylinder')
        .setRequired(true)
        .setMinValue(0.1)),
  
  async execute(interaction) {
    try {
      logger.info('Cylinder command execution started', {
        userId: interaction.user.id,
        guildId: interaction.guildId
      });
      
      // Immediately defer the reply to prevent timeout
      await interaction.deferReply();
      logger.debug('Reply deferred');
      
      const radius = interaction.options.getNumber('radius');
      const height = interaction.options.getNumber('height');
      
      logger.info('Cylinder parameters received', { radius, height });
      
      // Create parameters object
      const params = { radius, height };
      
      await interaction.editReply(`Generating cylinder model with:\nRadius: ${radius}\nHeight: ${height}`);
      
      try {
        // Generate the model
        const result = await cadService.generateModel('cylinder', params);
        logger.info('Model generation successful', { result });
        
        // Get the filename
        const fileName = result.fileName;
        
        // Try to render the model and get an image
        let imagePath = null;
        try {
          logger.info('Attempting to render the model', { fileName });
          imagePath = await cadService.renderModel(fileName, 'cylinder');
          logger.info('Rendering successful', { imagePath });
          
          // Send the rendered image
          const attachment = new AttachmentBuilder(imagePath, { name: 'cylinder_render.png' });
          
          await interaction.editReply({
            content: `Cylinder model generated successfully!\n` +
                    `Parameters: Radius: ${radius}, Height: ${height}`,
            files: [attachment]
          });
          
          logger.info('Sent message with rendered image');
        } catch (renderError) {
          logger.warn('Rendering failed, sending text-only response', { 
            error: renderError.message 
          });
          
          // Just send model info if rendering failed
          await interaction.editReply({
            content: `Cylinder model generated successfully!\n` +
                    `Parameters: Radius: ${radius}, Height: ${height}\n` +
                    `Model file: ${fileName}\n\n` +
                    `(Failed to render model image: ${renderError.message})`
          });
        }
      } catch (error) {
        throw error;
      }
    } catch (error) {
      await handleCommandError(error, interaction, 'cylinder', {
        params: { radius: interaction.options.getNumber('radius'), height: interaction.options.getNumber('height') }
      });
    }
  },
};