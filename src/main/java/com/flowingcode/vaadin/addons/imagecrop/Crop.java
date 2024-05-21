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

/**
 * Represents crop dimensions.
 * <p>
 * The crop dimensions are defined by the unit, x and y coordinates, width, and
 * height.
 *
 * @param unit   the unit of the crop dimensions, can be 'px' (pixels) or '%'
 *               (percentage).
 * @param x      the x-coordinate of the cropped area.
 * @param y      the y-coordinate of the cropped area.
 * @param width  the width of the cropped area
 * @param height the height of the cropped area
 */
public record Crop(String unit, int x, int y, int width, int height) {

    /**
     * Returns a string representation of the Crop object.
     *
     * @return A string representing the crop dimensions in the format:
     *         "{ unit: %s, x: %s, y: %s, width: %s, height: %s }"
     *         where %s is replaced by the corresponding value.
     */
    @Override
    public final String toString() {
        return "{ unit: %s, x: %s, y: %s, width: %s, height: %s }".formatted(unit, x, y, width, height);
    }

}
