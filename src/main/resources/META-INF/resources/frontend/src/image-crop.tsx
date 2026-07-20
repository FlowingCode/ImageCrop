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

		// Skip the first run of the output-format effect (initial encoding is handled on image load)
		const didMountRef = useRef(false);

		/**
		* Converts a value expressed in source (natural) pixels to a percentage of
		* the given dimension.
		*/
		const toPercent = (value: number, dimension: number) =>
			dimension ? (value / dimension) * 100 : 0;

		/**
		* Normalizes the configured crop when the image loads. The crop is kept as a
		* percentage of the image's natural size, so both the on-screen selection and
		* the exported image are independent of how the browser scales the image on
		* screen. A configured "px" crop is interpreted as source (natural) pixels,
		* which makes the exported size deterministic (see issue #33).
		*/
		const onImageLoad = () => {
			const img = imgRef.current;
			if (!img || !crop) {
				return;
			}
			const { naturalWidth, naturalHeight } = img;

			// Work in "%": a "px" crop is treated as source pixels and converted.
			let normalized: PercentCrop = crop.unit === "%"
				? { unit: "%", x: crop.x, y: crop.y, width: crop.width, height: crop.height }
				: {
					unit: "%",
					x: toPercent(crop.x, naturalWidth),
					y: toPercent(crop.y, naturalHeight),
					width: toPercent(crop.width, naturalWidth),
					height: toPercent(crop.height, naturalHeight),
				};

			// Enforce the aspect ratio when configured, then center the selection.
			if (aspect) {
				normalized = makeAspectCrop(
					{ unit: "%", width: normalized.width, x: normalized.x, y: normalized.y },
					aspect,
					naturalWidth,
					naturalHeight
				);
			}
			normalized = centerCrop(normalized, naturalWidth, naturalHeight);

			setCrop(normalized);
			this._updateCroppedImage(normalized);
		};

		/**
		* Normalizes a programmatic "px" crop that the server sets after the image
		* has loaded. onImageLoad only runs on the initial load, so without this a
		* later setCrop("px", ...) would be rendered by ReactCrop as on-screen pixels
		* and the selection box would diverge from the natural-pixel export (issue
		* #33). The configured x/y are preserved (no centering) since the crop is
		* explicitly positioned.
		*/
		useEffect(() => {
			const img = imgRef.current;
			if (crop && crop.unit !== "%" && img && img.naturalWidth) {
				setCrop({
					unit: "%",
					x: toPercent(crop.x, img.naturalWidth),
					y: toPercent(crop.y, img.naturalHeight),
					width: toPercent(crop.width, img.naturalWidth),
					height: toPercent(crop.height, img.naturalHeight),
				});
			}
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

		// Keep the crop state in "%" so it stays valid regardless of the image's
		// on-screen size; that scale-invariance is why no ResizeObserver is needed
		// to rescale it on layout changes (see issue #33).
		const onChange = (_pixelCrop: PixelCrop, percentCrop: PercentCrop) => {
			setCrop(percentCrop);
		};

		const onComplete = (_pixelCrop: PixelCrop, percentCrop: PercentCrop) => {
			this._updateCroppedImage(percentCrop);
		};
		
		return (
			<ReactCrop
				crop={crop}
				onChange={(c: PixelCrop, pc: PercentCrop) => onChange(c, pc)}
				onComplete={(c: PixelCrop, pc: PercentCrop) => onComplete(c, pc)}
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
	 * <p>The crop is mapped to the image's <em>natural</em> (intrinsic) pixels:
	 * {@code convertToPixelCrop} scales a {@code %} crop against
	 * {@code naturalWidth}/{@code naturalHeight}, while a {@code px} crop is taken
	 * as source (natural) pixels directly. Because the mapping never depends on the
	 * image's on-screen size, the exported dimensions are deterministic regardless
	 * of how the browser scaled the image when the crop was set (see issues #26 and
	 * #33).</p>
	 *
	 * <p>Note: the output is not multiplied by {@code window.devicePixelRatio}, so
	 * the original pixels are used verbatim instead of being upsampled on
	 * high-density displays (see issue #21).</p>
	 */
	public _updateCroppedImage(crop: PixelCrop|PercentCrop) {
			const image = this.querySelector("img");
			if (crop && image) {

				// Map the crop to the image's natural pixels. A "%" crop scales to the
				// natural resolution; a "px" crop is interpreted as source pixels.
				const ccrop = convertToPixelCrop(crop, image.naturalWidth, image.naturalHeight);

				// create a canvas element to draw the cropped image
				const canvas = document.createElement("canvas");
				const ctx = canvas.getContext("2d");

				// The output is sized in the crop region's natural pixels, so the cropped
				// image keeps the source's resolution rather than the on-screen size.
				const outWidth = Math.round(ccrop.width);
				const outHeight = Math.round(ccrop.height);

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
						ccrop.x,
						ccrop.y,
						ccrop.width,
						ccrop.height,
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