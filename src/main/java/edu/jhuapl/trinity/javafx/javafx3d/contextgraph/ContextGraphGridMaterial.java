package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.javafx.javafx3d.images.SolidColorTexture;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * Opaque/transparent 3D grid material using Trinity's established illumination-map
 * alpha path. Neither Node.opacity nor diffuse-color alpha works reliably for
 * partially transparent TriangleMesh geometry in the JavaFX Prism 3D pipeline.
 */
final class ContextGraphGridMaterial {
    private ContextGraphGridMaterial() { }

    static PhongMaterial forColor(Color value) {
        Color color = value == null ? Color.TRANSPARENT : value;
        PhongMaterial material = new PhongMaterial(null, null, null, null, null);
        material.setDiffuseColor(null);
        material.setDiffuseMap(null);
        material.setSelfIlluminationMap(SolidColorTexture.of(color));
        return material;
    }
}
