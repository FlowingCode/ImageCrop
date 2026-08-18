/*-
 * #%L
 * Image Crop Add-on
 * %%
 * Copyright (C) 2024-2025 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.flowingcode.vaadin.addons.imagecrop;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import com.vaadin.flow.shared.Registration;

/**
 * Component for cropping images based on
 * <a href="https://www.npmjs.com/package/react-image-crop">react-image-crop</a>
 * library.
 * This component allows users to define and manipulate crop areas on images.
 * 
 * @author Paola De Bartolo / Flowing Code
 */
@NpmPackage(value = "react-image-crop", version = "11.0.6")
@JsModule("./src/image-crop.tsx")
@Tag("image-crop")
@CssImport("react-image-crop/dist/ReactCrop.css")
@CssImport("./styles/image-crop-styles.css")
public class ImageCrop extends ReactAdapterComponent {
  
  private static final String IMG_FULL_HEIGHT_CLASS_NAME = "img-full-height";

  private String croppedImageDataUri;

  /**
   * Constructs an ImageCrop component with the given image URL.
   *
   * @param src the URL of the image to be cropped
   */
  public ImageCrop(String src) {
    this.setImageSrc(src);
    this.addCroppedImageListener(this::updateCroppedImage);
    this.croppedImageDataUri = src;
  }

  /**
   * Constructs an ImageCrop component with the given image.
   *
   * @param image the image to be cropped
   * @deprecated This constructor only preserves the image URL and {@linkplain #setImageAlt(String)
   *             alternate text}. Use {@link #ImageCrop(String)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.2.0")
  public ImageCrop(Image image) {
    this(image.getSrc());
    image.getAlt().ifPresent(a -> this.setImageAlt(a)); 
  }

  /**
   * Adds a listener for the {@link CroppedImageEvent} fired when the
   * cropped image is updated.
   *
   * @param listener the listener to be added
   * @return a registration for the listener, which can be used to remove the
   *         listener
   */
  protected Registration addCroppedImageListener(
      ComponentEventListener<CroppedImageEvent> listener) {
    return this.addListener(CroppedImageEvent.class, listener);
  }

  /**
   * Updates the cropped image data URI based on the event data.
   *
   * @param event the event containing the new cropped image data URI
   */
  private void updateCroppedImage(CroppedImageEvent event) {
    this.croppedImageDataUri = event.getCroppedImageDataUri();
  }

  /**
   * Sets the source of the image to be cropped.
   *
   * @param imageSrc the image source
   */
  public void setImageSrc(String imageSrc) {
    setState("imgSrc", imageSrc);
  }

  /**
   * Gets the source of the image being cropped.
   *
   * @return the image source
   */
  public String getImageSrc() {
    return getState("imgSrc", String.class);
  }

   /**
   * Sets the alternative information of the image to be cropped.
   *
   * @param imageAlt the image alternative information
   */
  public void setImageAlt(String imageAlt) {
    setState("imgAlt", imageAlt);
  }

  /**
   * Gets the alternative information of the image being cropped.
   *
   * @return the image alternative information
   */
  public String getImageAlt() {
    return getState("imgAlt", String.class);
  }

  /**
   * Defines the crop dimensions.
   *
   * <p>
   * A {@code %} crop is resolution-independent and is recommended for
   * programmatic use. A {@code px} crop is interpreted in the image's
   * <em>source</em> (natural) pixels, so the cropped output has a deterministic
   * size regardless of how the image is scaled on screen (see issue #33). Note
   * that this differs from {@link #setCropMinWidth(Integer) the min/max crop
   * constraints}, which are expressed in rendered (on-screen) pixels.
   *
   * <p>
   * A {@code %} crop is measured against the image's natural size when the output
   * is generated, but against its rendered box when the selection is drawn. Both
   * agree as long as the rendered image keeps the source's aspect ratio; forcing
   * both a width and a height on the image (or an {@code object-fit} that crops or
   * stretches it) makes the on-screen selection diverge from the exported region.
   *
   * @param crop the crop dimensions
   */
  public void setCrop(Crop crop) {
    setState("crop", crop);
    getElement().executeJs("this._updateCroppedImage(this.crop)");
  }

  /**
   * Gets the crop dimensions.
   *
   * <p>
   * Once the image has loaded, the crop is normalized to a percentage of the
   * image's natural size, so the returned unit is {@code %} even when
   * {@link #setCrop(Crop)} was given a {@code px} crop. Because {@link Crop} holds
   * integer values, those percentages are rounded to whole units, which on a large
   * image is coarser than the configured pixel values.
   *
   * @return the crop dimensions
   */
  public Crop getCrop() {
    return getState("crop", Crop.class);
  }

  /**
   * Sets the aspect ratio of the crop.
   * For example, 1 for a square or 16/9 for landscape.
   * 
   * @param aspect the aspect ratio of the crop
   */
  public void setAspect(double aspect) {
    setState("aspect", aspect);
  }

  /**
   * Gets the aspect ratio of the crop.
   *
   * @return the aspect ratio
   */
  public double getAspect() {
    return getState("aspect", Double.class);
  }

  /**
   * Sets whether the crop area should be shown as a circle.
   * If the aspect ratio is not 1, the circle will be warped into an oval shape.
   * Defaults to false.
   * 
   * @param circularCrop true to show the crop area as a circle, false otherwise
   */
  public void setCircularCrop(boolean circularCrop) {
    setState("circularCrop", circularCrop);
  }

  /**
   * Gets whether the crop area is shown as a circle.
   *
   * @return true if the crop area is a circle, false otherwise
   */
  public boolean isCircularCrop() {
    return getState("circularCrop", Boolean.class);
  }

  /**
   * Sets whether the selection can't be disabled if the user clicks outside
   * the selection area. Defaults to false.
   * 
   * @param keepSelection true so selection can't be disabled if the user clicks
   *                      outside the selection area, false otherwise.
   */
  public void setKeepSelection(boolean keepSelection) {
    setState("keepSelection", keepSelection);
  }

  /**
   * Gets whether the selection is enabled.
   *
   * @return true if the selection is enabled, false otherwise
   */
  public boolean isKeepSelection() {
    return getState("keepSelection", Boolean.class);
  }

  /**
   * Sets whether the user cannot resize or draw a new crop. Defaults to false.
   * 
   * @param disabled true to disable crop resizing and drawing, false otherwise
   */
  public void setDisabled(boolean disabled) {
    setState("disabled", disabled);
  }

  /**
   * Gets whether the crop resizing and drawing is disabled.
   *
   * @return true if disabled, false otherwise
   */
  public boolean isDisabled() {
    return getState("disabled", Boolean.class);
  }

  /**
   * Sets whether the user cannot create or resize a crop, but can still drag the
   * existing crop around. Defaults to false.
   * 
   * @param locked true to lock the crop, false otherwise
   */
  public void setLocked(boolean locked) {
    setState("locked", locked);
  }

  /**
   * Gets whether the crop is locked.
   *
   * @return true if the crop is locked, false otherwise
   */
  public boolean isLocked() {
    return getState("locked", Boolean.class);
  }

  /**
   * Sets a minimum crop width, in rendered (on-screen) pixels.
   *
   * <p>
   * This constraint is applied by react-image-crop in the image's displayed
   * pixels, unlike a {@code px} {@link #setCrop(Crop) crop}, which is expressed in
   * source (natural) pixels.
   *
   * @param minWidth the minimum crop width
   */
  public void setCropMinWidth(Integer minWidth) {
    setState("minWidth", minWidth);
  }

  /**
   * Gets the minimum crop width, in pixels.
   *
   * @return the minimum crop width
   */
  public Integer getCropMinWidth() {
    return getState("minWidth", Integer.class);
  }

  /**
   * Sets a minimum crop height, in pixels.
   * 
   * @param minHeight the minimum crop height
   */
  public void setCropMinHeight(Integer minHeight) {
    setState("minHeight", minHeight);
  }

  /**
   * Gets the minimum crop height, in pixels.
   *
   * @return the minimum crop height
   */
  public Integer getCropMinHeight() {
    return getState("minHeight", Integer.class);
  }

  /**
   * Sets a maximum crop width, in pixels.
   * 
   * @param maxWidth the maximum crop width
   */
  public void setCropMaxWidth(Integer maxWidth) {
    setState("maxWidth", maxWidth);
  }

  /**
   * Gets the maximum crop width, in pixels.
   *
   * @return the maximum crop width
   */
  public Integer getCropMaxWidth() {
    return getState("maxWidth", Integer.class);
  }

  /**
   * Sets a maximum crop height, in pixels.
   * 
   * @param maxHeight the maximum crop height
   */
  public void setCropMaxHeight(Integer maxHeight) {
    setState("maxHeight", maxHeight);
  }

  /**
   * Gets the maximum crop height, in pixels.
   *
   * @return the maximum crop height
   */
  public Integer getCropMaxHeight() {
    return getState("maxHeight", Integer.class);
  }

  /**
   * Sets whether to show rule of thirds lines in the cropped area. Defaults to
   * false.
   * 
   * @param ruleOfThirds true to show rule of thirds lines, false otherwise
   */
  public void setRuleOfThirds(boolean ruleOfThirds) {
    setState("ruleOfThirds", ruleOfThirds);
  }

  /**
   * Gets whether rule of thirds lines are shown in the cropped area.
   *
   * @return true if rule of thirds lines are shown, false otherwise
   */
  public boolean isRuleOfThirds() {
    return getState("ruleOfThirds", Boolean.class);
  }

  /**
   * Sets the MIME type used to encode the cropped image (for example
   * {@code "image/jpeg"} or {@code "image/png"}). Supported types are
   * {@code image/png}, {@code image/jpeg} and {@code image/webp}; any other or
   * unsupported value falls back to {@code image/png}.
   *
   * <p>
   * When left unset, the output format is auto-detected from the image source (its
   * data URL MIME type or file extension). Circular crops are always encoded in a
   * transparency-capable format regardless of this setting.
   *
   * @param outputMimeType the MIME type used to encode the cropped image
   */
  public void setOutputMimeType(String outputMimeType) {
    setState("outputMimeType", outputMimeType);
  }

  /**
   * Gets the MIME type used to encode the cropped image, or {@code null} if it is
   * auto-detected from the image source.
   *
   * @return the configured output MIME type, or {@code null}
   */
  public String getOutputMimeType() {
    if (getElement().getPropertyRaw("outputMimeType") == null) {
      return null;
    }
    return getState("outputMimeType", String.class);
  }

  /**
   * Sets the encoding quality used for lossy output formats such as
   * {@code image/jpeg} and {@code image/webp}. Ignored for {@code image/png}.
   * Defaults to {@code 1.0}.
   *
   * @param outputQuality a value between 0 and 1
   * @throws IllegalArgumentException if {@code outputQuality} is {@code NaN} or is
   *         outside the range {@code [0, 1]}
   */
  public void setOutputQuality(double outputQuality) {
    if (Double.isNaN(outputQuality) || outputQuality < 0 || outputQuality > 1) {
      throw new IllegalArgumentException(
          "outputQuality must be between 0 and 1, got: " + outputQuality);
    }
    setState("outputQuality", outputQuality);
  }

  /**
   * Gets the encoding quality used for lossy output formats.
   *
   * @return the output quality, between 0 and 1
   */
  public double getOutputQuality() {
    if (getElement().getPropertyRaw("outputQuality") == null) {
      return 1.0;
    }
    return getState("outputQuality", Double.class);
  }

  /**
   * Returns the cropped image data URI.
   *
   * @return the cropped image data URI
   */
  public String getCroppedImageDataUri() {
    return this.croppedImageDataUri;
  }

  /**
   * Sets the image to occupy the full viewport height when enabled.
   * If {@code fullHeight} is {@code true}, applies a CSS class that
   * sets the image height to 100vh. If {@code false}, removes the class
   * to revert to the default height.
   *
   * @param fullHeight whether the image should fill the viewport height
   */
  public void setImageFullHeight(Boolean fullHeight) {
    if (fullHeight)
      this.addClassName(IMG_FULL_HEIGHT_CLASS_NAME);
    else
      this.removeClassName(IMG_FULL_HEIGHT_CLASS_NAME);
  }

  /**
   * Decodes the cropped image data URI and returns it as a byte array. If the image data URI is not
   * in the format "data:image/*;base64,", it will be decoded assuming it is a Base64 encoded
   * string.
   * 
   * <p>
   * This method incorporates work licensed under MIT. Copyright 2021-2023 David "F0rce" Dodlek
   * https://github.com/F0rce/cropper
   * </p>
   * 
   * @return byte[] the decoded byte array of the cropped image
   */
  public byte[] getCroppedImageBase64() {
    String croppedDataUri = this.getCroppedImageDataUri();
    if (isBlank(croppedDataUri)) {
      return null;
    }

    String base64Data = croppedDataUri;
    if (croppedDataUri.contains("base64,")) {
      base64Data = croppedDataUri.split(",")[1];
    }

    return Base64.getDecoder().decode(base64Data.getBytes(StandardCharsets.UTF_8));
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

}
