const { SlashCommandBuilder, AttachmentBuilder } = require('discord.js');
const cadService = require('../services/cad-service');
const path = require('path');

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
    await interaction.deferReply();
    
    try {
      const outerDiameter = interaction.options.getNumber('outer_diameter');
      const innerDiameter = interaction.options.getNumber('inner_diameter');
      const thickness = interaction.options.getNumber('thickness');
      
      // Validate parameters
      if (innerDiameter >= outerDiameter) {
        return interaction.editReply('Error: Inner diameter must be less than outer diameter.');
      }
      
      // Create parameters object
      const params = {
        outer_diameter: outerDiameter,
        inner_diameter: innerDiameter,
        thickness: thickness
      };
      
      await interaction.editReply(`Generating washer model with:\nOuter Diameter: ${outerDiameter}\nInner Diameter: ${innerDiameter}\nThickness: ${thickness}`);
      
      // Generate the model
      const result = await cadService.generateModel('washer', params);
      
      // Extract the filename from the result
      const fileName = result.obj_path?.replace(/\.obj$/i, '') || 
                       result.obj_result?.file?.replace(/\.obj$/i, '') ||
                       `washer_${outerDiameter}_${innerDiameter}_${thickness}`;
      
      // Try to render the model and get an image
      let imagePath = null;
      let renderingSuccessful = false;
      
      try {
        // This will throw an error until the rendering endpoint is implemented
        imagePath = await cadService.renderModel(fileName, 'washer');
        renderingSuccessful = true;
      } catch (renderError) {
        console.log('Rendering not implemented yet:', renderError.message);
      }
      
      if (renderingSuccessful && imagePath) {
        // Send the image if rendering was successful
        const attachment = new AttachmentBuilder(imagePath, { name: 'washer_render.png' });
        
        await interaction.editReply({
          content: `Washer model generated successfully!\n` +
                  `Parameters: Outer Diameter: ${outerDiameter}, Inner Diameter: ${innerDiameter}, Thickness: ${thickness}`,
          files: [attachment]
        });
      } else {
        // Just send the model info if rendering wasn't successful
        await interaction.editReply({
          content: `Washer model generated successfully!\n` +
                  `Parameters: Outer Diameter: ${outerDiameter}, Inner Diameter: ${innerDiameter}, Thickness: ${thickness}\n` +
                  `Model file: ${fileName}\n\n` +
                  `(Rendering not available yet - implement the rendering endpoint in the API to see images)`
        });
      }
      
    } catch (error) {
      console.error('Error handling washer command:', error);
      await interaction.editReply(`Error: ${error.message}`);
    }
  },
};