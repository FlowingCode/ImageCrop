/*-
 * #%L
 * Image Crop Add-on
 * %%
 * Copyright (C) 2024 Flowing Code
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

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;

/**
 * Represents an event triggered when an image is cropped and encoded.
 */
@DomEvent("cropped-image")
public class CroppedImageEvent extends ComponentEvent<ImageCrop> {

  private String croppedImageDataUri;

  /**
   * Constructs a new CroppedImageEvent.
   *
   * @param source the source of the event
   * @param fromClient <code>true</code> if the event originated from the client-side,
   *        <code>false</code> otherwise
   * @param croppedImageDataUri the data URL of the cropped image
   */
  public CroppedImageEvent(ImageCrop source, boolean fromClient,
      @EventData("event.detail.croppedImageDataUri") String croppedImageDataUri) {
    super(source, fromClient);
    this.croppedImageDataUri = croppedImageDataUri;
  }

  /**
   * Returns the cropped image data URL.
   *
   * @return the cropped image data URL
   */
  public String getCroppedImageDataUri() {
    return this.croppedImageDataUri;
  }
}
