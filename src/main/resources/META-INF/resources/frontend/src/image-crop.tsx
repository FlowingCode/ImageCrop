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

import { ReactAdapterElement, RenderHooks } from 'Frontend/generated/flow/ReactAdapter';
import { JSXElementConstructor, ReactElement, useRef, useEffect } from "react";
import React from 'react';
import { type Crop, ReactCrop, PixelCrop, PercentCrop, makeAspectCrop, centerCrop, convertToPixelCrop } from "react-image-crop";

class ImageCropElement extends ReactAdapterElement {

	// Cache detected MIME per source URL to avoid repeated network calls
	#mimeTypeCache = new Map<string, string | null>();

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
		
		// Track previous image dimensions to adjust crop proportionally when resizing
		const prevImgSize = useRef<{ width: number; height: number } | null>(null);

		/**
		* Handles intial calculations on image load.
		*/
		const onImageLoad = () => {
			if (imgRef.current) {
				const { width, height } = imgRef.current;
				prevImgSize.current = { width, height };
				if (crop) {
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
					this._updateCroppedImage(newcrop).catch(console.error);
				}
			}
		};

		/**
		* Adjusts the crop size proportionally when the image is resized.
		*/
		const resizeCrop = (newWidth: number, newHeight: number) => {
			if (!crop || !prevImgSize.current) return;
			const { width: oldWidth, height: oldHeight } = prevImgSize.current;

			const scaleX = newWidth / oldWidth;
			const scaleY = newHeight / oldHeight;

			const resizedCrop: Crop = {
				unit: crop.unit,
				width: crop.width * scaleX,
				height: crop.height * scaleY,
				x: crop.x * scaleX,
				y: crop.y * scaleY,
			};

			setCrop(resizedCrop);
			prevImgSize.current = { width: newWidth, height: newHeight };
		};

		/**
		* Observes image resizing and updates crop size dynamically.
		*/
		useEffect(() => {
			if (!imgRef.current) return;

			const resizeObserver = new ResizeObserver(() => {
				if (imgRef.current && prevImgSize.current) {
					const { width, height } = imgRef.current;
					if (width != prevImgSize.current.width &&
						height != prevImgSize.current.height) {
						resizeCrop(width, height);
					}
				}
			});

			resizeObserver.observe(imgRef.current);

			return () => resizeObserver.disconnect();
		}, [crop]);

		const onChange = (c: Crop) => {
			setCrop(c);
		};

		const onComplete = (c: PixelCrop) => {
			this._updateCroppedImage(c).catch(console.error);
		};
		
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

	/**
	 * Attempts to detect the MIME type of given HTMLImageElement.
	 *
	 * Resolution order:
	 *  1. If the image is a data URL, extracts the MIME type directly.
	 *  2. Otherwise, sends a HEAD request to the image URL to read
	 *     the Content-Type header (faster, no body download).
	 *  3. If the HEAD request does not provide Content-Type, falls back
	 *     to fetching the full image as a Blob and using `blob.type`.
	 *
	 * @param img The HTMLImageElement whose MIME type should be detected.
	 * @returns A Promise resolving to the MIME type string (e.g. "image/png"),
	 *          or null if it cannot be determined.
	 */
	async #getImageMimeType(img: HTMLImageElement): Promise<string | null> {
		if (!img.src) {
			return null;
		}

		// Return cached result if available
		const cacheKey = img.src;
		const cached = this.#mimeTypeCache?.get(cacheKey);
		if (cached !== undefined){
			return cached;
		}

		// Case 1: data URL (e.g., data:image/png;base64,...)
		if (img.src.startsWith("data:")) {
			const semiIndex = img.src.indexOf(";");
			if (semiIndex > 5) {
				const mimeType = img.src.substring(5, semiIndex);
				this.#mimeTypeCache?.set(cacheKey, mimeType);
				return mimeType;
			}
			this.#mimeTypeCache?.set(cacheKey, null);
			return null;
		}

		try {
			// Case 2: try a HEAD request (fast, no body)
			const headRes = await fetch(img.src, { method: "HEAD" });
			let mimeType = headRes.headers.get("Content-Type");
			if (mimeType) {
				this.#mimeTypeCache?.set(cacheKey, mimeType);
				return mimeType;
			}

			// Case 3: fallback — fetch full blob
			const blobRes = await fetch(img.src);
			const blob = await blobRes.blob();
			mimeType = blob.type || null;
			this.#mimeTypeCache?.set(cacheKey, mimeType);
			return mimeType;
		} catch (err) {
			console.error("Error fetching image MIME type:", err);
			this.#mimeTypeCache?.set(cacheKey, null);
			return null;
		}
	}
	
	public async _updateCroppedImage(crop: PixelCrop|PercentCrop) {
			const image = this.querySelector("img");
			if (crop && image) {
				crop = convertToPixelCrop(crop, image.width, image.height);
				
				// create a canvas element to draw the cropped image
				const canvas = document.createElement("canvas");

				// draw the image on the canvas
				const ccrop = crop;
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

					if (this.circularCrop) {
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

					const imgMimeType = await this.#getImageMimeType(image) || 'image/png';

					// get the cropped image
					let croppedImageDataUri = canvas.toDataURL(imgMimeType, 1.0);

					// dispatch the event containing cropped image
					this.fireCroppedImageEvent(croppedImageDataUri);
				}
			}
	}
}

customElements.define("image-crop", ImageCropElement);