package com.norrisjackson.jsnippets.services;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the IdenticonService.
 */
class IdenticonServiceTest {

    private final IdenticonService identiconService = new IdenticonService();

    @Test
    void generateIdenticon_returnsValidPngImage() throws IOException {
        byte[] imageData = identiconService.generateIdenticon("test@example.com");

        assertThat(imageData).isNotNull();
        assertThat(imageData.length).isGreaterThan(0);

        // Verify it's a valid PNG
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(128); // Default size
        assertThat(image.getHeight()).isEqualTo(128);
    }

    @Test
    void generateIdenticon_withCustomSize_returnsCorrectSize() throws IOException {
        byte[] imageData = identiconService.generateIdenticon("test@example.com", 64);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        assertThat(image.getWidth()).isEqualTo(64);
        assertThat(image.getHeight()).isEqualTo(64);
    }

    @Test
    void generateIdenticon_sameEmailProducesSameImage() {
        byte[] image1 = identiconService.generateIdenticon("test@example.com");
        byte[] image2 = identiconService.generateIdenticon("test@example.com");

        assertThat(image1).isEqualTo(image2);
    }

    @Test
    void generateIdenticon_differentEmailsProduceDifferentImages() {
        byte[] image1 = identiconService.generateIdenticon("user1@example.com");
        byte[] image2 = identiconService.generateIdenticon("user2@example.com");

        assertThat(image1).isNotEqualTo(image2);
    }

    @Test
    void generateIdenticon_caseInsensitive() {
        byte[] image1 = identiconService.generateIdenticon("TEST@EXAMPLE.COM");
        byte[] image2 = identiconService.generateIdenticon("test@example.com");

        assertThat(image1).isEqualTo(image2);
    }

    @Test
    void generateIdenticon_trimsWhitespace() {
        byte[] image1 = identiconService.generateIdenticon("  test@example.com  ");
        byte[] image2 = identiconService.generateIdenticon("test@example.com");

        assertThat(image1).isEqualTo(image2);
    }

    @Test
    void resizeImage_resizesToCorrectDimensions() throws IOException {
        byte[] original = identiconService.generateIdenticon("test@example.com", 128);
        byte[] resized = identiconService.resizeImage(original, 32, 32);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(resized));
        assertThat(image.getWidth()).isEqualTo(32);
        assertThat(image.getHeight()).isEqualTo(32);
    }

    @Test
    void resizeImage_withInvalidData_throwsException() {
        byte[] invalidData = "not an image".getBytes();

        assertThatThrownBy(() -> identiconService.resizeImage(invalidData, 32, 32))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid image data");
    }

    @Test
    void generateIdenticon_withSmallSize_works() throws IOException {
        byte[] imageData = identiconService.generateIdenticon("test@example.com", 16);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        assertThat(image.getWidth()).isEqualTo(16);
    }

    @Test
    void generateIdenticon_withLargeSize_works() throws IOException {
        byte[] imageData = identiconService.generateIdenticon("test@example.com", 256);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        assertThat(image.getWidth()).isEqualTo(256);
    }
}

