package com.norrisjackson.jsnippets.services;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service for generating identicon images from email addresses.
 * Creates a unique, deterministic icon based on the MD5 hash of the email.
 */
@Service
public class IdenticonService {

    private static final int DEFAULT_SIZE = 128;
    private static final int GRID_SIZE = 5;

    /**
     * Generate an identicon PNG image for the given email address.
     *
     * @param email the email address to generate an identicon for
     * @param size the size of the output image in pixels (width and height)
     * @return PNG image data as byte array
     */
    public byte[] generateIdenticon(String email, int size) {
        byte[] hash = md5(email.toLowerCase().trim());

        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        // Use hash bytes to determine background and foreground colors
        Color backgroundColor = new Color(240, 240, 240); // Light gray background
        Color foregroundColor = new Color(
            hash[0] & 0xFF,
            hash[1] & 0xFF,
            hash[2] & 0xFF
        );

        // Ensure foreground is not too light
        if (isColorTooLight(foregroundColor)) {
            foregroundColor = foregroundColor.darker().darker();
        }

        // Fill background
        graphics.setColor(backgroundColor);
        graphics.fillRect(0, 0, size, size);

        // Draw the identicon pattern
        graphics.setColor(foregroundColor);
        int cellSize = size / GRID_SIZE;

        // Generate a symmetric pattern (mirror left to right)
        for (int x = 0; x < (GRID_SIZE + 1) / 2; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                // Use hash bytes to determine if cell is filled
                int hashIndex = (x * GRID_SIZE + y) % hash.length;
                if ((hash[hashIndex] & 0x01) == 1) {
                    // Draw on the left side
                    graphics.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                    // Mirror to the right side
                    int mirrorX = GRID_SIZE - 1 - x;
                    if (mirrorX != x) {
                        graphics.fillRect(mirrorX * cellSize, y * cellSize, cellSize, cellSize);
                    }
                }
            }
        }

        graphics.dispose();

        // Convert to PNG bytes
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate identicon", e);
        }
    }

    /**
     * Generate an identicon with the default size.
     *
     * @param email the email address
     * @return PNG image data
     */
    public byte[] generateIdenticon(String email) {
        return generateIdenticon(email, DEFAULT_SIZE);
    }

    /**
     * Resize an image to the specified dimensions.
     *
     * @param imageData the original image data
     * @param width target width
     * @param height target height
     * @return resized PNG image data
     */
    public byte[] resizeImage(byte[] imageData, int width, int height) {
        try {
            BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageData));
            if (originalImage == null) {
                throw new IllegalArgumentException("Invalid image data");
            }

            BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = resizedImage.createGraphics();

            // Use high-quality rendering hints
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            graphics.drawImage(originalImage, 0, 0, width, height, null);
            graphics.dispose();

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(resizedImage, "PNG", baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to resize image", e);
        }
    }

    private byte[] md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(input.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    private boolean isColorTooLight(Color color) {
        // Calculate perceived brightness
        double brightness = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return brightness > 0.7;
    }
}

