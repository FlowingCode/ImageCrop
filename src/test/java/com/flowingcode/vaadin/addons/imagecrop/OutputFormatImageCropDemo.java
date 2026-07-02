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

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Image Crop Output Format")
@SuppressWarnings("serial")
@Route(value = "image-crop/output-format", layout = ImageCropDemoView.class)
public class OutputFormatImageCropDemo extends VerticalLayout {

  private static final String AUTO_DETECT = "Auto-detect";

  private Div croppedResultDiv = new Div();

  public OutputFormatImageCropDemo() {
    add(new Span("Choose an output format and crop the image. "
        + "When left on \"Auto-detect\", the format is inferred from the image source."));

    ImageCrop imageCrop = new ImageCrop("images/empty-plant.png");
    imageCrop.setCrop(new Crop("%", 25, 25, 50, 50)); // start with a centered selection

    // Pick the MIME type used to encode the cropped image.
    Select<String> formatSelect = new Select<>();
    formatSelect.setLabel("Output format");
    formatSelect.setItems(AUTO_DETECT, "image/png", "image/jpeg", "image/webp");
    formatSelect.setValue(AUTO_DETECT);
    formatSelect.addValueChangeListener(e -> {
      String value = e.getValue();
      // Changing the format re-encodes the current crop automatically (client-side effect).
      imageCrop.setOutputMimeType(AUTO_DETECT.equals(value) ? null : value);
    });

    add(formatSelect, imageCrop);

    Button getCropButton = new Button("Get Cropped Image");
    croppedResultDiv.setId("result-cropped-image-div");

    getCropButton.addClickListener(e -> {
      croppedResultDiv.removeAll();
      String dataUri = imageCrop.getCroppedImageDataUri();
      Span mimeType = new Span("Result MIME type: " + extractMimeType(dataUri));
      mimeType.getStyle().set("display", "block");
      croppedResultDiv.add(mimeType);
      croppedResultDiv.add(new Image(dataUri, "cropped image"));
    });

    add(getCropButton, new Span("Crop Result:"), croppedResultDiv);
  }

  /** Reads the MIME type from the prefix of a {@code data:} URI, for display purposes. */
  private static String extractMimeType(String dataUri) {
    if (dataUri != null && dataUri.startsWith("data:")) {
      int sep = dataUri.indexOf(';', 5);
      if (sep < 0) {
        sep = dataUri.indexOf(',', 5);
      }
      if (sep > 5) {
        return dataUri.substring(5, sep);
      }
    }
    return "unknown";
  }

}
