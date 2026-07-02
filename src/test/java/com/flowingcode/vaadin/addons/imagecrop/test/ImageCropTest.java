package com.flowingcode.vaadin.addons.imagecrop.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Base64;

import org.junit.Before;
import org.junit.Test;

import com.flowingcode.vaadin.addons.imagecrop.Crop;
import com.flowingcode.vaadin.addons.imagecrop.CroppedImageEvent;
import com.flowingcode.vaadin.addons.imagecrop.ImageCrop;

public class ImageCropTest {

  private ImageCrop imageCrop;

  @Before
  public void setUp() {
    imageCrop = new ImageCrop("dummyImageSrc");
  }

  @Test
  public void testSetAndGetImageSrc() {
    String expectedSrc = "newImageSrc";
    imageCrop.setImageSrc(expectedSrc);
    assertEquals(expectedSrc, imageCrop.getImageSrc());
  }

  @Test
  public void testSetAndGetCrop() {
    Crop expectedCrop = new Crop("%", 10, 10, 200, 200);
    imageCrop.setCrop(expectedCrop);
    assertEquals(expectedCrop, imageCrop.getCrop());
  }

  @Test
  public void testSetAndGetAspect() {
    Double expectedAspect = 16.0 / 9.0;
    imageCrop.setAspect(expectedAspect);
    assertEquals(expectedAspect, Double.valueOf(imageCrop.getAspect()));
  }

  @Test
  public void testSetAndGetOutputMimeType() {
    String expectedMimeType = "image/jpeg";
    imageCrop.setOutputMimeType(expectedMimeType);
    assertEquals(expectedMimeType, imageCrop.getOutputMimeType());
  }

  @Test
  public void testSetAndGetOutputQuality() {
    double expectedQuality = 0.8;
    imageCrop.setOutputQuality(expectedQuality);
    assertEquals(expectedQuality, imageCrop.getOutputQuality(), 0.0);
  }

  @Test
  public void testGetOutputMimeTypeDefaultsToNullWhenUnset() {
    assertNull(imageCrop.getOutputMimeType());
  }

  @Test
  public void testGetOutputQualityDefaultsToOneWhenUnset() {
    assertEquals(1.0, imageCrop.getOutputQuality(), 0.0);
  }

  @Test
  public void testEncodedCroppedImageEvent() {
    String expectedCroppedImageUri = "croppedImageUri";
    CroppedImageEvent event = mock(CroppedImageEvent.class);
    when(event.getCroppedImageDataUri()).thenReturn(expectedCroppedImageUri);
    imageCrop = mock(ImageCrop.class);
    when(imageCrop.getCroppedImageDataUri()).thenReturn(expectedCroppedImageUri);
    assertEquals(expectedCroppedImageUri, imageCrop.getCroppedImageDataUri());
  }

  @Test
  public void testGetCroppedImageBase64() {
    byte[] expectedCroppedImageBytes = Base64.getDecoder().decode("SGVsbG8gV29ybGQ=");
    imageCrop = mock(ImageCrop.class);
    when(imageCrop.getCroppedImageBase64()).thenReturn(expectedCroppedImageBytes);
    byte[] actualCroppedImageBytes = imageCrop.getCroppedImageBase64();
    assertNotNull(actualCroppedImageBytes);
    assertArrayEquals(expectedCroppedImageBytes, actualCroppedImageBytes);
  }
}
