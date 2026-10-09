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

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;

@DemoSource
@PageTitle("Image Crop with DownloadHandler")
@SuppressWarnings("serial")
@Route(value = "image-crop/download-handler", layout = ImageCropDemoView.class)
public class DownloadHandlerImageCropDemo extends VerticalLayout {

  private Div croppedResultDiv = new Div();

  public DownloadHandlerImageCropDemo() {
    add(new Span("Select a portion of the picture to crop: "));

    // The image is served by a DownloadHandler registered to the ImageCrop itself
    DownloadHandler handler = DownloadHandler.forClassResource(getClass(),
        "/META-INF/resources/images/empty-plant.png", "empty-plant.png").inline();
    ImageCrop imageCrop = new ImageCrop(handler);
    imageCrop.setImageAlt("image to crop");
    add(imageCrop);

    Button getCropButton = new Button("Get Cropped Image");

    croppedResultDiv.setId("result-cropped-image-div");

    getCropButton.addClickListener(e -> {
      croppedResultDiv.removeAll();
      // there is no cropped image until the user selects an area
      if (imageCrop.getCroppedImageDataUri() != null) {
        croppedResultDiv.add(new Image(imageCrop.getCroppedImageDataUri(), "cropped image"));
      }
    });

    add(getCropButton, new Span("Crop Result:"), croppedResultDiv);
  }

}
