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

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@DemoSource
@PageTitle("Image Crop with Upload")
@SuppressWarnings("serial")
@Route(value = "image-crop/upload", layout = ImageCropDemoView.class)
public class UploadImageCropDemo extends Div {

  private static final String[] ACCEPTED_MIME_TYPES =
      {"image/gif", "image/png", "image/jpeg", "image/bmp", "image/webp"};

  private Avatar avatar = new Avatar();
  private ImageCrop imageCrop = null;
  private byte[] newCroppedPicture = null;

  public UploadImageCropDemo() {
    avatar.addThemeVariants(AvatarVariant.LUMO_XLARGE);
    avatar.setHeight("12em");
    avatar.setWidth("12em");
    Div avatarDiv = new Div(avatar);

    MemoryBuffer buffer = new MemoryBuffer();
    Upload uploadComponent = new Upload(buffer);
    uploadComponent.setMaxFiles(1);
    uploadComponent.setMaxFileSize(1024 * 1024 * 10);
    uploadComponent.setAcceptedFileTypes(ACCEPTED_MIME_TYPES);

    Span uploadCaption = new Span("Upload an image to crop and set as avatar:");

    HorizontalLayout avatarLayout =
        new HorizontalLayout(avatarDiv, new VerticalLayout(uploadCaption, uploadComponent));

    avatarLayout.setAlignItems(Alignment.CENTER);

    uploadComponent.addFinishedListener(e -> {
      openCropDialog(((ByteArrayOutputStream) buffer.getFileData().getOutputBuffer()),
          e.getMIMEType());
    });

    uploadComponent.addFileRejectedListener(event -> {
      String errorMessage = event.getErrorMessage();
      Notification notification =
          Notification.show(errorMessage, 5000, Notification.Position.MIDDLE);
      notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    });

    add(avatarLayout);
  }

  private void openCropDialog(ByteArrayOutputStream outputStream, String mimeType) {
    // Set up image crop dialog
    Dialog dialog = new Dialog();
    dialog.setCloseOnOutsideClick(false);
    dialog.setMaxHeight("100%");
    dialog.setMaxWidth(dialog.getHeight());

    Button cropButton = new Button("Crop image");
    Button dialogCancelButton = new Button("Cancel");
    dialogCancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

    String src = getImageAsBase64(outputStream.toByteArray(), mimeType);
    imageCrop = new ImageCrop(src);
    imageCrop.setAspect(1.0);
    imageCrop.setCircularCrop(true);
    imageCrop.setCrop(new Crop("%", 25, 25, 50, 50)); // centered crop
    imageCrop.setKeepSelection(true);

    cropButton.addClickListener(event -> {
      newCroppedPicture = imageCrop.getCroppedImageBase64();
      avatar.setImage(imageCrop.getCroppedImageDataUri());
      dialog.close();
    });
    dialogCancelButton.addClickListener(c -> dialog.close());

    HorizontalLayout buttonLayout = new HorizontalLayout(dialogCancelButton, cropButton);
    Div dialogLayout = new Div(imageCrop);
    dialogLayout.setSizeFull();
    buttonLayout.setWidthFull();
    buttonLayout.setJustifyContentMode(JustifyContentMode.END);
    dialog.add(dialogLayout);
    dialog.getFooter().add(buttonLayout);
    dialog.open();
  }

  private String getImageAsBase64(byte[] src, String mimeType) {
    return src != null ? "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(src)
        : null;
  }
}
