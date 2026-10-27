/*-
 * #%L
 * Image Crop Add-on
 * %%
 * Copyright (C) 2024-2026 Flowing Code
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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;

/** Shows the cropped image of a demo, or asks the user to select an area first. */
@SuppressWarnings("serial")
class CroppedImageResult extends Div {

  CroppedImageResult() {
    setId("result-cropped-image-div");
  }

  /**
   * Replaces the content with the given cropped image.
   *
   * @param croppedImageDataUri the cropped image, or {@code null} if there is none yet
   * @return whether there was a cropped image to show
   */
  boolean show(String croppedImageDataUri) {
    removeAll();
    // there is no cropped image until the user selects an area
    if (croppedImageDataUri == null) {
      add(new Span("Select an area first"));
      return false;
    }
    add(new Image(croppedImageDataUri, "cropped image"));
    return true;
  }
}
