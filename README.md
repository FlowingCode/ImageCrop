[![Published on Vaadin Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://vaadin.com/directory/component/image-crop-add-on)
[![Stars on vaadin.com/directory](https://img.shields.io/vaadin-directory/star/image-crop-add-on.svg)](https://vaadin.com/directory/component/image-crop-add-on)
[![Build Status](https://jenkins.flowingcode.com/job/ImageCrop-addon/badge/icon)](https://jenkins.flowingcode.com/job/ImageCrop-addon)
[![Maven Central](https://img.shields.io/maven-central/v/com.flowingcode.vaadin.addons/image-crop-addon)](https://mvnrepository.com/artifact/com.flowingcode.vaadin.addons/image-crop-addon)

# Image Crop Add-on

Component for cropping images. Wrapper for React component [react-image-crop](https://www.npmjs.com/package/react-image-crop).

## Features

The component allows to crop images and configure the following properties for a customized crop:
* crop dimensions (unit, x and y coordinates, width, and height) 
* aspect ratio (for example, 1 for a square or 16/9 for landscape)
* circular crop (selection are has circular shape)
* keep selection (selection can't be disabled if the user clicks outside the selection area)
* disabled (cannot resize or draw a new crop)
* locked (cannot create or resize a crop, but can still drag the existing crop around)
* min width (minimum crop width)
* min height (minimum crop height)
* max width (maximum crop width)
* max height (maximum crop height)
* rule of thirds (to show rule of thirds lines in the cropped area)

The cropped image result can be obtain as a URI using `getCroppedImageDataUri` method
or as a Base64 encoded byte array by using `getCroppedImageBase64` method.

## Online demo

[Online demo here](http://addonsv24.flowingcode.com/image-crop)

## Download release

[Available in Vaadin Directory](https://vaadin.com/directory/component/image-crop-add-on)

### Maven install

Add the following dependencies in your pom.xml file:

```xml
<dependency>
   <groupId>com.flowingcode.vaadin.addons</groupId>
   <artifactId>image-crop-addon</artifactId>
   <version>X.Y.Z</version>
</dependency>
```
<!-- the above dependency should be updated with latest released version information -->

```xml
<repository>
   <id>vaadin-addons</id>
   <url>https://maven.vaadin.com/vaadin-addons</url>
</repository>
```

For SNAPSHOT versions see [here](https://maven.flowingcode.com/snapshots/).

## Building and running demo

- git clone repository
- mvn clean install jetty:run

To see the demo, navigate to http://localhost:8080/

## Release notes

See [here](https://github.com/FlowingCode/ImageCrop/releases)

## Issue tracking

The issues for this add-on are tracked on its github.com page. All bug reports and feature requests are appreciated. 

## Contributions

Contributions are welcome, but there are no guarantees that they are accepted as such. 

As first step, please refer to our [Development Conventions](https://github.com/FlowingCode/DevelopmentConventions) page to find information about Conventional Commits & Code Style requirements.

Then, follow these steps for creating a contribution:

- Fork this project.
- Create an issue to this project about the contribution (bug or feature) if there is no such issue about it already. Try to keep the scope minimal.
- Develop and test the fix or functionality carefully. Only include minimum amount of code needed to fix the issue.
- For commit message, use [Conventional Commits](https://github.com/FlowingCode/DevelopmentConventions/blob/main/conventional-commits.md) to describe your change.
- Send a pull request for the original project.
- Comment on the original issue that you have implemented a fix for it.

## License & Author

This add-on is distributed under Apache License 2.0. For license terms, see LICENSE.txt.

Image Crop Add-on is written by Flowing Code S.A.

# Developer Guide

## Getting started

* Basic use

```java
Image image = new Image("images/empty-plant.png", "image to crop");  
ImageCrop imageCrop = new ImageCrop(image);
add(imageCrop);
```

* Get cropped image 

```java
Image croppedImage = new Image(imageCrop.getCroppedImageDataUri(), "cropped image")
```

## Special configuration when using Spring

By default, Vaadin Flow only includes ```com/vaadin/flow/component``` to be always scanned for UI components and views. For this reason, the add-on might need to be whitelisted in order to display correctly. 

To do so, just add ```com.flowingcode``` to the ```vaadin.allowed-packages``` property in ```src/main/resources/application.properties```, like:

```vaadin.allowed-packages = com.vaadin,org.vaadin,dev.hilla,com.flowingcode```
 
More information on Spring whitelisted configuration [here](https://vaadin.com/docs/latest/integrations/spring/configuration/#configure-the-scanning-of-packages).
