const { SlashCommandBuilder, AttachmentBuilder } = require('discord.js');
const cadService = require('../services/cad-service');
const path = require('path');

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
      // Immediately defer the reply to prevent timeout
      await interaction.deferReply();
      
      const radius = interaction.options.getNumber('radius');
      const height = interaction.options.getNumber('height');
      
      // Create parameters object
      const params = {
        radius: radius,
        height: height
      };
      
      await interaction.editReply(`Generating cylinder model with:\nRadius: ${radius}\nHeight: ${height}`);
      
      try {
        // Generate the model
        const result = await cadService.generateModel('cylinder', params);
        
        // Extract the filename from the result
        const fileName = result.obj_path?.replace(/\.obj$/i, '') || 
                        result.obj_result?.file?.replace(/\.obj$/i, '') ||
                        `cylinder_${radius}_${height}`;
        
        // Try to render the model and get an image
        let imagePath = null;
        let renderingSuccessful = false;
        
        try {
          // This will throw an error until the rendering endpoint is implemented
          imagePath = await cadService.renderModel(fileName, 'cylinder');
          renderingSuccessful = true;
        } catch (renderError) {
          console.log('Rendering not implemented yet:', renderError.message);
        }
        
        if (renderingSuccessful && imagePath) {
          // Send the image if rendering was successful
          const attachment = new AttachmentBuilder(imagePath, { name: 'cylinder_render.png' });
          
          await interaction.editReply({
            content: `Cylinder model generated successfully!\n` +
                    `Parameters: Radius: ${radius}, Height: ${height}`,
            files: [attachment]
          });
        } else {
          // Just send the model info if rendering wasn't successful
          await interaction.editReply({
            content: `Cylinder model generated successfully!\n` +
                    `Parameters: Radius: ${radius}, Height: ${height}\n` +
                    `Model file: ${fileName}\n\n` +
                    `(Rendering not available yet - implement the rendering endpoint in the API to see images)`
          });
        }
      } catch (error) {
        console.error('Error in model generation:', error);
        await interaction.editReply(`Error: ${error.message}`);
      }
    } catch (error) {
      console.error('Error handling cylinder command:', error);
      
      // Only try to respond if the interaction is still valid
      if (error.code !== 10062) {
        try {
          if (interaction.deferred) {
            await interaction.editReply(`Error: ${error.message}`);
          } else if (!interaction.replied) {
            await interaction.reply(`Error: ${error.message}`);
          }
        } catch (replyError) {
          console.error('Error sending error response:', replyError);
        }
      }
    }
  },
};