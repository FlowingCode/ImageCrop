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

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Basic Image Crop")
@SuppressWarnings("serial")
@Route(value = "image-crop/basic", layout = ImageCropDemoView.class)
public class BasicImageCropDemo extends VerticalLayout {

  private Div croppedResultDiv = new Div();

  public BasicImageCropDemo() {
    add(new Span("Select a portion of the picture to crop: "));

    Image image = new Image("images/empty-plant.png", "image to crop");
    ImageCrop imageCrop = new ImageCrop(image);
    add(imageCrop);

    Button getCropButton = new Button("Get Cropped Image");

    croppedResultDiv.setId("result-cropped-image-div");
    croppedResultDiv.setWidth(image.getWidth());
    croppedResultDiv.setHeight(image.getHeight());

    getCropButton.addClickListener(e -> {
      croppedResultDiv.removeAll();
      croppedResultDiv.add(new Image(imageCrop.getCroppedImageDataUri(), "cropped image"));
    });

    add(getCropButton, new Span("Crop Result:"), croppedResultDiv);
  }

}
