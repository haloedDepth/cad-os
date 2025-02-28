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
      console.log('Cylinder command execution started');
      
      // Immediately defer the reply to prevent timeout
      await interaction.deferReply();
      console.log('Reply deferred');
      
      const radius = interaction.options.getNumber('radius');
      const height = interaction.options.getNumber('height');
      
      console.log(`Received parameters: radius=${radius}, height=${height}`);
      
      // Create parameters object
      const params = {
        radius: radius,
        height: height
      };
      
      await interaction.editReply(`Generating cylinder model with:\nRadius: ${radius}\nHeight: ${height}`);
      console.log('Updated reply with parameter information');
      
      try {
        console.log('Calling CAD service to generate model...');
        // Generate the model
        const result = await cadService.generateModel('cylinder', params);
        console.log('Model generation successful. Result:', result);
        
        // Extract the filename from the result
        const fileName = result.obj_path?.replace(/\.obj$/i, '') || 
                        result.obj_result?.file?.replace(/\.obj$/i, '') ||
                        `cylinder_${radius}_${height}`;
        
        console.log(`Extracted file name: ${fileName}`);
        
        // Try to render the model and get an image
        let imagePath = null;
        let renderingSuccessful = false;
        
        try {
          console.log('Attempting to render the model...');
          // This will throw an error until the rendering endpoint is implemented
          imagePath = await cadService.renderModel(fileName, 'cylinder');
          renderingSuccessful = true;
          console.log('Rendering successful. Image path:', imagePath);
        } catch (renderError) {
          console.log('Rendering not implemented yet:', renderError.message);
        }
        
        if (renderingSuccessful && imagePath) {
          console.log('Preparing to send the rendered image...');
          // Send the image if rendering was successful
          const attachment = new AttachmentBuilder(imagePath, { name: 'cylinder_render.png' });
          
          await interaction.editReply({
            content: `Cylinder model generated successfully!\n` +
                    `Parameters: Radius: ${radius}, Height: ${height}`,
            files: [attachment]
          });
          console.log('Sent message with rendered image');
        } else {
          console.log('Rendering not available, sending text-only response');
          // Just send the model info if rendering wasn't successful
          await interaction.editReply({
            content: `Cylinder model generated successfully!\n` +
                    `Parameters: Radius: ${radius}, Height: ${height}\n` +
                    `Model file: ${fileName}\n\n` +
                    `(Rendering not available yet - implement the rendering endpoint in the API to see images)`
          });
          console.log('Sent text-only response');
        }
      } catch (error) {
        console.error('Error in model generation:', error);
        console.error('Stack trace:', error.stack);
        // Craft a more helpful error message
        let errorMessage = `Error: ${error.message}\n\nTroubleshooting:\n`;
        
        if (error.message.includes('ECONNREFUSED') && error.message.includes('::1')) {
          errorMessage += "• IPv6 connection issue detected. The server is configured to use IPv6 (::1) but can't connect.\n";
          errorMessage += "• Try updating your .env file to set API_BASE_URL=http://127.0.0.1:8000/api to force IPv4.\n";
        } else if (error.message.includes('ECONNREFUSED') && error.message.includes('127.0.0.1')) {
          errorMessage += "• Make sure the FastAPI server is running on port 8000.\n";
          errorMessage += "• Check that there are no firewall rules blocking the connection.\n";
        }
        
        errorMessage += "• Verify that both API and Clojure services are running.";
        
        await interaction.editReply(errorMessage);
        console.log('Sent error message to user');
      }
    } catch (error) {
      console.error('Error handling cylinder command:', error);
      console.error('Stack trace:', error.stack);
      
      // Only try to respond if the interaction is still valid
      if (error.code !== 10062) {
        try {
          if (interaction.deferred) {
            // Craft a more helpful error message
            let errorMessage = `Error: ${error.message}\n\nTroubleshooting:\n`;
            
            if (error.message.includes('ECONNREFUSED') && error.message.includes('::1')) {
              errorMessage += "• IPv6 connection issue detected. The server is configured to use IPv6 (::1) but can't connect.\n";
              errorMessage += "• Try updating your .env file to set API_BASE_URL=http://127.0.0.1:8000/api to force IPv4.\n";
            } else if (error.message.includes('ECONNREFUSED') && error.message.includes('127.0.0.1')) {
              errorMessage += "• Make sure the FastAPI server is running on port 8000.\n";
              errorMessage += "• Check that there are no firewall rules blocking the connection.\n";
            }
            
            errorMessage += "• Verify that both API and Clojure services are running.";
            
            await interaction.editReply(errorMessage);
            console.log('Sent error message to user after defer');
          } else if (!interaction.replied) {
            await interaction.reply(`Error: ${error.message}\n\nMake sure the FastAPI server is running on port 8000.`);
            console.log('Sent error message to user directly');
          }
        } catch (replyError) {
          console.error('Error sending error response:', replyError);
        }
      }
    }
  },
};