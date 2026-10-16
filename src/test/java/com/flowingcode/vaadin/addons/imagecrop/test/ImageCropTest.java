package com.flowingcode.vaadin.addons.imagecrop.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.flowingcode.vaadin.addons.imagecrop.Crop;
import com.flowingcode.vaadin.addons.imagecrop.CroppedImageEvent;
import com.flowingcode.vaadin.addons.imagecrop.ImageCrop;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
public class ImageCropTest {

  private ImageCrop imageCrop;

  // keeps the current UI strongly reachable during the test
  private UI ui;

  @Before
  public void setUp() {
    ui = new UI();
    UI.setCurrent(ui);
    imageCrop = new ImageCrop("dummyImageSrc");
  }

  @Test
  public void testSetAndGetImageSrc() {
    String expectedSrc = "newImageSrc";
    imageCrop.setImageSrc(expectedSrc);
    assertEquals(expectedSrc, imageCrop.getImageSrc());
  }

  @Test
  public void testSetImageSrcWithDownloadHandler() {
    imageCrop.setImageSrc(imageHandler());
    assertNotNull(imageCrop.getImageSrc());
    assertEquals(imageCrop.getImageSrc(), imageCrop.getElement().getAttribute("img-src"));
  }

  @Test
  public void testSetImageSrcWithStringReleasesDownloadHandler() {
    imageCrop.setImageSrc(imageHandler());
    imageCrop.setImageSrc("newImageSrc");
    assertEquals("newImageSrc", imageCrop.getImageSrc());
    assertFalse(imageCrop.getElement().hasAttribute("img-src"));
  }

  @Test
  public void testSetImageSrcWithDownloadHandlerResetsCroppedImage() {
    imageCrop.setImageSrc(imageHandler());
    assertNull(imageCrop.getCroppedImageDataUri());
  }

  @Test
  public void testSetImageSrcWithStringResetsCroppedImage() {
    ComponentUtil.fireEvent(imageCrop,
        new CroppedImageEvent(imageCrop, true, "data:image/png;base64,SGVsbG8gV29ybGQ="));
    assertNotNull(imageCrop.getCroppedImageDataUri());
    imageCrop.setImageSrc("newImageSrc");
    assertNull(imageCrop.getCroppedImageDataUri());
    assertNull(imageCrop.getCroppedImageBase64());
  }

  @Test
  public void testSetSameImageSrcKeepsCroppedImage() {
    String croppedImageDataUri = "data:image/png;base64,SGVsbG8gV29ybGQ=";
    ComponentUtil.fireEvent(imageCrop, new CroppedImageEvent(imageCrop, true, croppedImageDataUri));
    imageCrop.setImageSrc(imageCrop.getImageSrc());
    assertEquals(croppedImageDataUri, imageCrop.getCroppedImageDataUri());
  }

  @Test
  public void testSetImageSrcWithResourceUrlResetsCroppedImage() {
    imageCrop.setImageSrc(imageHandler());
    ComponentUtil.fireEvent(imageCrop,
        new CroppedImageEvent(imageCrop, true, "data:image/png;base64,SGVsbG8gV29ybGQ="));
    // passing the resource URL back unregisters the handler, so the image changes
    imageCrop.setImageSrc(imageCrop.getImageSrc());
    assertNull(imageCrop.getCroppedImageDataUri());
  }

  @Test
  public void testStringConstructorHasNoCroppedImage() {
    imageCrop = new ImageCrop("images/empty-plant.png");
    assertNull(imageCrop.getCroppedImageDataUri());
    assertNull(imageCrop.getCroppedImageBase64());
  }

  @Test
  public void testDownloadHandlerConstructorHasNoCroppedImage() {
    imageCrop = new ImageCrop(imageHandler());
    assertNotNull(imageCrop.getImageSrc());
    assertNull(imageCrop.getCroppedImageDataUri());
    assertNull(imageCrop.getCroppedImageBase64());
  }

  @After
  public void tearDown() {
    UI.setCurrent(null);
  }

  private static DownloadHandler imageHandler() {
    return DownloadHandler.fromInputStream(event -> new DownloadResponse(
        new ByteArrayInputStream(new byte[0]), "image.png", "image/png", 0)).inline();
  }

  @Test
  public void testSetAndGetCrop() {
    Crop expectedCrop = new Crop("%", 10, 10, 200, 200);
    imageCrop.setCrop(expectedCrop);
    assertEquals(expectedCrop, imageCrop.getCrop());
  }

  @Test
  public void testGetCropKeepsSubPercentPrecision() {
    // the client syncs the crop as a "%" crop with fractional values; on a
    // 4000px-wide image 1% is 40px, so dropping the fraction moves the selection
    Crop expectedCrop = new Crop("%", 12.3456, 7.89, 33.3333, 50.125);
    imageCrop.setCrop(expectedCrop);

    Crop crop = imageCrop.getCrop();
    assertEquals(expectedCrop, crop);

    imageCrop.setCrop(crop);
    assertEquals(crop, imageCrop.getCrop());
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
