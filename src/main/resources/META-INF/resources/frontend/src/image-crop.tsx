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

// MIME types that HTMLCanvasElement.toDataURL can actually encode across browsers.
// Anything else silently falls back to image/png, so we never emit it.
const SUPPORTED_OUTPUT_TYPES = new Set(["image/png", "image/jpeg", "image/webp"]);

/** Normalizes a MIME type: lowercased, parameters stripped (e.g. "image/JPEG; charset=x" -> "image/jpeg"). */
function normalizeMimeType(mime: string | null | undefined): string | undefined {
	if (!mime) {
		return undefined;
	}
	const normalized = mime.toLowerCase().split(";")[0].trim();
	return normalized || undefined;
}

/**
 * Zero-network detection of the image MIME type from its source: reads it from a
 * data URL prefix, or infers it from the file extension of a regular URL.
 */
function detectMimeTypeFromSrc(src: string | null | undefined): string | undefined {
	if (!src) {
		return undefined;
	}
	if (src.startsWith("data:")) {
		// e.g. "data:image/png;base64,..." or "data:image/svg+xml,..."
		const sep = src.search(/[;,]/);
		return sep > 5 ? normalizeMimeType(src.substring(5, sep)) : undefined;
	}
	// Strip query/hash, then read the file extension.
	const path = src.split(/[?#]/)[0];
	const ext = path.substring(path.lastIndexOf(".") + 1).toLowerCase();
	switch (ext) {
		case "png": return "image/png";
		case "jpg":
		case "jpeg": return "image/jpeg";
		case "webp": return "image/webp";
		default: return undefined;
	}
}

/**
 * Resolves the MIME type used to encode the cropped output.
 *
 * Uses the explicitly requested type when given (normalized + validated against
 * the supported set); otherwise auto-detects it from the image source. Falls
 * back to "image/png" when the result is empty or unsupported. Circular crops
 * are forced to a transparency-capable format, since JPEG has no alpha channel
 * and would render the rounded corners black.
 */
function resolveOutputType(requested: string | null | undefined, src: string | null | undefined, circular: boolean): string {
	let type = normalizeMimeType(requested);
	if (!type) {
		type = detectMimeTypeFromSrc(src);
	}
	if (!type || !SUPPORTED_OUTPUT_TYPES.has(type)) {
		type = "image/png";
	}
	if (circular && type === "image/jpeg") {
		type = "image/png";
	}
	return type;
}

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
		// Output format settings; also read via this.outputMimeType / this.outputQuality at crop time.
		const [outputMimeType] = hooks.useState<string>("outputMimeType");
		const [outputQuality] = hooks.useState<number>("outputQuality", 1.0);

		// Track previous image dimensions to adjust crop proportionally when resizing
		const prevImgSize = useRef<{ width: number; height: number } | null>(null);
		// Skip the first run of the output-format effect (initial encoding is handled on image load)
		const didMountRef = useRef(false);

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
					this._updateCroppedImage(newcrop);
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

		/**
		* Re-encodes the current crop on the client when the output format or quality
		* changes, so the result stays in sync without relying on server-side
		* state/JS ordering. Skips the initial mount (handled by onImageLoad).
		*/
		useEffect(() => {
			if (!didMountRef.current) {
				didMountRef.current = true;
				return;
			}
			if (crop) {
				this._updateCroppedImage(crop);
			}
		}, [outputMimeType, outputQuality]);

		const onChange = (c: Crop) => {
			setCrop(c);
		};

		const onComplete = (c: PixelCrop) => {
			this._updateCroppedImage(c);
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
	 * Draws the selected crop region onto an off-screen canvas and dispatches the
	 * resulting data URI through a {@code cropped-image} event.
	 *
	 * <p>The crop rectangle reported by react-image-crop is expressed in the
	 * image's <em>displayed</em> (rendered) pixels, which can be smaller or larger
	 * than the image's intrinsic resolution when the browser scales it to fit the
	 * layout. The selected region is mapped back to the source's <em>natural</em>
	 * pixels using {@code scaleX}/{@code scaleY} for both the source rectangle and
	 * the output canvas, so the cropped image keeps the original resolution of the
	 * selected area rather than the (smaller or larger) on-screen size (see issue
	 * #26).</p>
	 *
	 * <p>Note: a {@code px} crop is measured in rendered pixels, so the exported
	 * size is the rendered crop scaled to natural resolution, not necessarily the
	 * configured pixel value. The output is not multiplied by
	 * {@code window.devicePixelRatio}, so the original pixels are used verbatim
	 * instead of being upsampled on high-density displays (see issue #21).</p>
	 */
	public _updateCroppedImage(crop: PixelCrop|PercentCrop) {
			const image = this.querySelector("img");
			if (crop && image) {

				crop = convertToPixelCrop(crop, image.width, image.height);

				// create a canvas element to draw the cropped image
				const canvas = document.createElement("canvas");

				// draw the image on the canvas
				const ccrop = crop;

				// Ratio between the image's natural resolution and its displayed size.
				// Greater than 1 when the image is scaled down to fit the screen.
				const scaleX = image.naturalWidth / image.width;
				const scaleY = image.naturalHeight / image.height;
				const ctx = canvas.getContext("2d");

				// Size the output in the crop region's natural pixels so the cropped
				// image keeps the source's resolution rather than the on-screen size.
				const outWidth = Math.round(ccrop.width * scaleX);
				const outHeight = Math.round(ccrop.height * scaleY);

				// Setting canvas dimensions resets the 2D context, so it must happen
				// before any drawing/clipping state is configured below.
				canvas.width = outWidth;
				canvas.height = outHeight;

				if (ctx) {
					ctx.imageSmoothingQuality = "high";
					ctx.save();

					if (this.circularCrop) {
						ctx.beginPath();
						ctx.arc(outWidth / 2, outHeight / 2, outHeight / 2, 0, Math.PI * 2, true);
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
						outWidth,
						outHeight
					);

					ctx.restore();

					// encode the cropped image using the resolved output format
					const outputType = resolveOutputType(this.outputMimeType, image.src, this.circularCrop);
					let croppedImageDataUri = canvas.toDataURL(outputType, this.outputQuality ?? 1.0);

					// dispatch the event containing cropped image
					this.fireCroppedImageEvent(croppedImageDataUri);
				}
			}
	}
}

customElements.define("image-crop", ImageCropElement);