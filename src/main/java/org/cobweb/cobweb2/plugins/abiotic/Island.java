package org.cobweb.cobweb2.plugins.abiotic;

import org.cobweb.io.ConfDisplayName;
import org.cobweb.io.ConfXMLTag;


public class Island extends AbioticFactor {

	@ConfDisplayName("Island value")
	@ConfXMLTag("center")
	public float islandValue = 1f;

	@ConfDisplayName("Outside value")
	@ConfXMLTag("outside")
	public float outsideValue = 0f;

	@ConfDisplayName("Island size X")
	@ConfXMLTag("sizeX")
	public float sizeX = 0.5f;

	@ConfDisplayName("Island size Y")
	@ConfXMLTag("sizeY")
	public float sizeY = 0.5f;

	@ConfDisplayName("Position X")
	@ConfXMLTag("positionX")
	public float positionX = 0.4f;

	@ConfDisplayName("Position Y")
	@ConfXMLTag("positionY")
	public float positionY = 0.5f;

	@ConfDisplayName("Edge hardness")
	@ConfXMLTag("transition")
	public float transition = 0.3f;

	@Override
	public float getValue(float x, float y) {
		x -= positionX;
		y -= positionY;

		x /= sizeX / 2;
		y /= sizeY / 2;

		float distance = 1 - (x*x + y*y);
		float insidePart =  1.0f / (float) (1 + Math.exp(-distance * 10 * transition));
		if (insidePart < 0)
			insidePart = 0;
		else if (insidePart > 1)
			insidePart = 1;

		return islandValue * insidePart + outsideValue * (1 - insidePart);
	}

	@Override
	public float getMax() {
		return Math.max(islandValue, outsideValue);
	}

	@Override
	public float getMin() {
		return Math.min(islandValue, outsideValue);
	}

	@Override
	public String getName() {
		return "Island";
	}

	@Override
	public AbioticFactor copy() {
		try {
			return (Island) this.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}


	private static final long serialVersionUID = 1L;
}
