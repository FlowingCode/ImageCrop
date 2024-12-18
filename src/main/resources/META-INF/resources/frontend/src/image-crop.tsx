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

import { ReactAdapterElement, RenderHooks } from 'Frontend/generated/flow/ReactAdapter';
import { JSXElementConstructor, ReactElement, useRef } from "react";
import React from 'react';
import { type Crop, ReactCrop, PixelCrop, makeAspectCrop, centerCrop } from "react-image-crop";

class ImageCropElement extends ReactAdapterElement {

	protected render(hooks: RenderHooks): ReactElement<any, string | JSXElementConstructor<any>> | null {

		const [crop, setCrop] = hooks.useState<Crop>("crop");
		const [imgSrc] = hooks.useState<string>("imgSrc");
		const imgRef = useRef<HTMLImageElement>(null);
		const [imgAlt] = hooks.useState<string>("imgAlt");
		const [aspect] = hooks.useState<number>("aspect");
		const [circularCrop] = hooks.useState<boolean>("circularCrop", false);
		const [keepSelection] = hooks.useState<boolean>("keepSelection", false);
		const [disabled] = hooks.useState<boolean>("disabled", false);
		const [locked] = hooks.useState<boolean>("locked", false);
		const [minWidth] = hooks.useState<number>("minWidth");
		const [minHeight] = hooks.useState<number>("minHeight");
		const [maxWidth] = hooks.useState<number>("maxWidth");
		const [maxHeight] = hooks.useState<number>("maxHeight");
		const [ruleOfThirds] = hooks.useState<boolean>("ruleOfThirds", false);

		const onImageLoad = () => {
			if (imgRef.current && crop) {
				const { width, height } = imgRef.current;
				const newcrop = centerCrop(
					makeAspectCrop(
						{
							unit: crop.unit,
							width: crop.width,
							height: crop.height,
							x: crop.x,
							y: crop.y
						},
						aspect,
						width,
						height
					),
					width,
					height
				)
				setCrop(newcrop);
			}
		};

		const onChange = (c: Crop) => {
			setCrop(c);
		};

		const onComplete = (c: PixelCrop) => {
			croppedImageEncode(c);
		};

		const croppedImageEncode = (completedCrop: PixelCrop) => {
			if (completedCrop) {

				// get the image element
				const image = imgRef.current;

				// create a canvas element to draw the cropped image
				const canvas = document.createElement("canvas");

				// draw the image on the canvas
				if (image) {
					const ccrop = completedCrop;
					const scaleX = image.naturalWidth / image.width;
					const scaleY = image.naturalHeight / image.height;
					const ctx = canvas.getContext("2d");
					const pixelRatio = window.devicePixelRatio;
					canvas.width = ccrop.width * pixelRatio;
					canvas.height = ccrop.height * pixelRatio;

					if (ctx) {
						ctx.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
						ctx.imageSmoothingQuality = "high";

						ctx.save();

						if (circularCrop) {
							canvas.width = ccrop.width;
							canvas.height = ccrop.height;

							ctx.beginPath();

							ctx.arc(ccrop.width / 2, ccrop.height / 2, ccrop.height / 2, 0, Math.PI * 2, true);
							ctx.closePath();
							ctx.clip();
						}

						ctx.drawImage(
							image,
							ccrop.x * scaleX,
							ccrop.y * scaleY,
							ccrop.width * scaleX,
							ccrop.height * scaleY,
							0,
							0,
							ccrop.width,
							ccrop.height
						);

						ctx.restore();
					}

					// get the cropped image
					let croppedImageDataUri = canvas.toDataURL("image/png", 1.0);

					// dispatch the event containing cropped image
					this.fireCroppedImageEvent(croppedImageDataUri);
				}
			}
		}

		return (
			<ReactCrop
				crop={crop}
				onChange={(c: Crop) => onChange(c)}
				onComplete={(c: PixelCrop) => onComplete(c)}
				circularCrop={circularCrop}
				aspect={aspect}
				keepSelection={keepSelection}
				disabled={disabled}
				locked={locked}
				minWidth={minWidth}
				minHeight={minHeight}
				maxWidth={maxWidth}
				maxHeight={maxHeight}
				ruleOfThirds={ruleOfThirds}
			>
				<img
					ref={imgRef}
					src={imgSrc}
					alt={imgAlt}
					onLoad={onImageLoad} />
			</ReactCrop>
		);
	}

	private fireCroppedImageEvent(croppedImageDataUri: string) {
		this.dispatchEvent(
			new CustomEvent("cropped-image", {
				detail: {
					croppedImageDataUri: croppedImageDataUri
				},
			})
		);
	}
}

customElements.define("image-crop", ImageCropElement);