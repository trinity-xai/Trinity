package edu.jhuapl.trinity.utils.fun.solar;

import java.time.LocalTime;
import javafx.geometry.Point2D;

/**
 *
 * @author Sean Phillips
 */
public enum SunPathMode {
    ELLIPTICAL {
        @Override
        public Point2D computePosition(double t, double paneW, double paneH, double arcW, double arcH) {
            double angle = 2 * Math.PI * t;
            double dx = (paneW * arcW) * Math.cos(angle);
            double dy = -(paneH * arcH) * Math.sin(angle);
            return new Point2D(dx, dy);
        }
    },
    HORIZONTAL {
        @Override
        public Point2D computePosition(double t, double paneW, double paneH, double arcW, double arcH) {
            double dx = -(paneW * arcW) * Math.sin(2 * Math.PI * t);
            return new Point2D(dx, -(paneH * arcH));
        }
    },
    VERTICAL {
        @Override
        public Point2D computePosition(double t, double paneW, double paneH, double arcW, double arcH) {
            double dy = -(paneH * arcH) * Math.sin(2 * Math.PI * t);
            return new Point2D((paneW * arcW), dy);
        }
    },
    ANGULAR_TIME_OF_DAY {
        @Override
        public Point2D computePosition(double t, double paneW, double paneH, double arcW, double arcH) {
            // Simulate time of day from 0 (midnight) to 1 (next midnight)
            double sunAngle = Math.toRadians(180 * t); // rises in the east, sets in the west
            double radius = Math.min(paneW * arcW, paneH * arcH);
            double dx = radius * Math.cos(sunAngle - Math.PI / 2); // start from bottom
            double dy = radius * Math.sin(sunAngle - Math.PI / 2);
            return new Point2D(dx, dy);
        }
    },
    REALTIME_SUN_ARC {
        @Override
        public Point2D computePosition(double unusedT, double paneW, double paneH, double arcW, double arcH) {
            // Get current local time (0.0 = midnight, 1.0 = next midnight)
            LocalTime now = LocalTime.now();
            double secondsInDay = now.toSecondOfDay();
            double timeOfDay = secondsInDay / 86400.0; // normalize [0, 1]

            // Define day duration: sun is visible between 6 AM and 6 PM (12h span)
            double sunrise = 5.0 / 24.0;
            double sunset = 21.0 / 24.0;

            double t = (timeOfDay - sunrise) / (sunset - sunrise); // normalize to [0, 1] daytime
            t = Math.max(0, Math.min(t, 1)); // clamp

            // Use t to compute angle in upper semicircle [180° to 0°]
            double angle = Math.PI * (1 - t); // PI (left horizon) → 0 (right horizon)

            double radiusX = paneW * arcW / 2.0;
            double radiusY = paneH * arcH / 2.0;

            double dx = radiusX * Math.cos(angle);
            double dy = -radiusY * Math.sin(angle); // negative because Y grows downward in JavaFX

            return new Point2D(dx, dy);
        }
    };

    public abstract Point2D computePosition(double t, double paneW, double paneH, double arcW, double arcH);
}
