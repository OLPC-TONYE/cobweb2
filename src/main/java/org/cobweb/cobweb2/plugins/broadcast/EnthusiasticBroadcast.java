package org.cobweb.cobweb2.plugins.broadcast;

import org.cobweb.cobweb2.core.Location;
import org.cobweb.cobweb2.impl.ComplexAgent;


public class EnthusiasticBroadcast extends BroadcastPacket {

	public Location breedLocation;

	public EnthusiasticBroadcast(Location breedLocation, ComplexAgent dispatcherId) {
		super(dispatcherId);
		this.breedLocation = breedLocation;
	}

	@Override
	public void process(ComplexAgent receiver) {
		double closeness = 1;

		if (!breedLocation.equals(receiver.getPosition()))
			closeness = 1 / receiver.environment.topology.getDistance(receiver.getPosition(), breedLocation);

		receiver.setCommInbox(closeness);

	}

}
