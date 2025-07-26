// app.paralelafinal.ui.TrafficLightVisuals
package app.paralelafinal.escenario1.simulation;

import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class TrafficLightVisuals {
    private final Circle redLight;
    private final Circle greenLight;

    public TrafficLightVisuals(Circle redLight, Circle greenLight) {
        this.redLight = redLight;
        this.greenLight = greenLight;
    }

    public void updateLight(boolean isGreen) {
        if (isGreen) {
            greenLight.setFill(Color.LIMEGREEN);
            greenLight.setEffect(new Glow(0.8));
            redLight.setFill(Color.DARKRED.desaturate());
            redLight.setEffect(null);
        } else {
            redLight.setFill(Color.RED);
            redLight.setEffect(new Glow(0.8));
            greenLight.setFill(Color.DARKGREEN.desaturate());
            greenLight.setEffect(null);
        }
    }
}