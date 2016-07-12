package org.oscim.layers.vector.geometries;

import org.oscim.backend.canvas.Color;

/**
 * Class encapsulating style information for drawing geometries on the map.
 */
public class Style {

    public static final int GENERALIZATION_HIGH = 1 << 3;
    public static final int GENERALIZATION_MEDIUM = 1 << 2;
    public static final int GENERALIZATION_SMALL = 1 << 0;
    public static final int GENERALIZATION_NONE = 0;

    public final float strokeWidth;
    public final int strokeColor;

    public final int fillColor;
    public final float fillAlpha;

    public final double buffer;
    public final int scalingZoomLevel;

    public final int generalization;

    private Style(Builder builder) {
        strokeWidth = builder.strokeWidth;
        strokeColor = builder.strokeColor;

        fillColor = builder.fillColor;
        fillAlpha = builder.fillAlpha;

        buffer = builder.buffer;
        scalingZoomLevel = builder.scalingZoomLevel;

        generalization = builder.generalization;
    }

    /**
     * Geometry style builder. Usage example:
     * <p/>
     * <pre>
     * {
     *     Style style = Style.builder()
     *         .strokeWidth(1f).strokeColor(Color.BLACK).build();
     * }
     * </pre>
     */
    public static class Builder {

        private float strokeWidth = 1f;
        private int strokeColor = Color.GRAY;
        private int fillColor = Color.GRAY;
        private float fillAlpha = 0.25f;

        private double buffer = 1;
        private int scalingZoomLevel = 1;

        private int generalization = GENERALIZATION_NONE;

        protected Builder() {

        }

        /**
         * Builds the GeometryStyle from the specified parameters.
         *
         * @return
         */
        public Style build() {
            return new Style(this);
        }

        /**
         * Sets the line width for the geometry's line or outline.
         *
         * @param lineWidth
         * @return
         */
        public Builder strokeWidth(float lineWidth) {
            this.strokeWidth = lineWidth;
            return this;
        }

        /**
         * Sets the color for the geometry's line or outline.
         *
         * @param stokeColor
         * @return
         */
        public Builder strokeColor(int stokeColor) {
            this.strokeColor = stokeColor;
            return this;
        }

        /**
         * Sets the color for the geometry's area.
         *
         * @param fillColor
         * @return
         */
        public Builder fillColor(int fillColor) {
            this.fillColor = fillColor;
            return this;
        }

        /**
         * Sets alpha channel value for the geometry's area.
         *
         * @param fillAlpha
         * @return
         */
        public Builder fillAlpha(double fillAlpha) {
            this.fillAlpha = (float) fillAlpha;
            return this;
        }

        /**
         * This function has effect only on Points:
         * use it to control the size on the circle that
         * will be built from a buffer around the point.
         *
         * @param buffer
         * @return itself
         */
        public Builder buffer(double buffer) {
            this.buffer = buffer;
            return this;
        }

        /**
         * This function has effect only on Points:
         * use it to specify from which zoom level the point
         * should stop decreasing in size and "stick to the map".
         *
         * @param zoomlvl
         */
        public Builder scaleZoomLevel(int zoomlvl) {
            this.scalingZoomLevel = zoomlvl;
            return this;
        }

        /**
         * Sets generalization factor for the geometry.
         * Use predefined GeometryStyle.GENERALIZATION_HIGH,
         * GENERALIZATION_MEDIUM or GENERALIZATION_SMALL
         *
         * @param generalization
         * @return
         */
        public Builder generalization(int generalization) {
            this.generalization = generalization;
            return this;
        }
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public int getStrokeColor() {
        return strokeColor;
    }

    public int getFillColor() {
        return fillColor;
    }

    public float getFillAlpha() {
        return fillAlpha;
    }

    public int getGeneralization() {
        return generalization;
    }

    public double getBuffer() {
        return buffer;
    }

    public int getScalingZoomLevel() {
        return scalingZoomLevel;
    }

    static final Style DEFAULT_STYLE = new Builder()
            .fillColor(0xcccccccc)
            .fillAlpha(1)
            .build();

    public static Style defaultStyle() {
        return DEFAULT_STYLE;
    }

    public static Style.Builder builder() {
        return new Style.Builder();
    }

}
