/**
 *
 */
package org.cobweb.cobweb2.plugins.disease;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.cobweb.cobweb2.core.Agent;
import org.cobweb.cobweb2.core.SimulationTimeSpace;
import org.cobweb.cobweb2.plugins.ContactMutator;
import org.cobweb.cobweb2.plugins.LoggingMutator;
import org.cobweb.cobweb2.plugins.SpawnMutator;
import org.cobweb.cobweb2.plugins.StatefulMutatorBase;
import org.cobweb.cobweb2.plugins.UpdateMutator;
import org.cobweb.util.ArrayUtilities;

/**
 * Simulates various diseases that can affect agents.
 */
public class DiseaseMutator extends StatefulMutatorBase<DiseaseState> implements ContactMutator, SpawnMutator, LoggingMutator, UpdateMutator {

	public DiseaseParams params;

	private int sickCount[] = new int[0];

	private SimulationTimeSpace simulation;

	public DiseaseMutator() {
		super(DiseaseState.class);
	}

	@Override
	public Collection<String> logDataAgent(int agentType) {
		List<String> l = new LinkedList<String>();
		l.add(Integer.toString(sickCount[agentType]));
		return l;
	}

	@Override
	public Collection<String> logDataTotal() {
		List<String> l = new LinkedList<String>();
		int sum = 0;
		for(int x : sickCount)
			sum += x;

		l.add(Integer.toString(sum));
		return l;
	}

	@Override
	public Collection<String> logHeadersAgent() {
		List<String> header = new LinkedList<String>();
		header.add("Diseased");
		return header;
	}

	@Override
	public Collection<String> logHeaderTotal() {
		List<String> header = new LinkedList<String>();
		header.add("Diseased");
		return header;
	}

	private void makeRandomSick(Agent agent, float rate) {
		boolean isSick = false;
		if (simulation.getRandom().nextFloat() < rate)
			isSick = true;

		if (isSick) {
			DiseaseAgentParams agentParams = params.agentParams[agent.getType()];
			agentParams.param.modifyValue(this, agent, agentParams.factor);

			sickCount[agent.getType()]++;

			setAgentState(agent, new DiseaseState(agentParams, true, false, simulation.getTime()));
		}

	}

	private void makeRandomVaccinated(Agent agent, float rate) {
		boolean isVaccinated = false;
		if (simulation.getRandom().nextFloat() < rate) {
			isVaccinated = true;
		}
		if (isVaccinated) {
			DiseaseAgentParams agentParams = params.agentParams[agent.getType()];
			setAgentState(agent, new DiseaseState(agentParams, false, true, agentParams.vaccineEffectiveness));
		}
	}


	public void contactTransmit(Agent agent, float rate) {
		boolean isSick = false;
		if (simulation.getRandom().nextFloat() < rate)
			isSick = true;

		if (isSick) {
			DiseaseAgentParams agentParams = params.agentParams[agent.getType()];
			agentParams.param.modifyValue(this, agent, agentParams.factor);

			sickCount[agent.getType()]++;

			if (isVaccinated(agent)) {
				setAgentState(agent, new DiseaseState(agentParams, true, true, simulation.getTime()));
			}
			else {
				setAgentState(agent, new DiseaseState(agentParams, true, false, simulation.getTime()));
			}
		}
	}

	@Override
	public void onContact(Agent bumper, Agent bumpee) {
		transmitBumpOneWay(bumper, bumpee);
		transmitBumpOneWay(bumpee, bumper);
	}

	@Override
	public void onDeath(Agent agent) {
		DiseaseState diseaseState = removeAgentState(agent);
		if (diseaseState != null && diseaseState.sick)
			sickCount[agent.getType()]--;
	}

	@Override
	public void onSpawn(Agent agent) {
		makeRandomVaccinated(agent, params.agentParams[agent.getType()].initialVaccination);
		if (!isVaccinated(agent)) {
			makeRandomSick(agent, params.agentParams[agent.getType()].initialInfection);
		}
	}

	@Override
	public void onSpawn(Agent agent, Agent parent) {
		if (parent.isAlive() && isSick(parent))
			makeRandomSick(agent, params.agentParams[agent.getType()].childTransmitRate);
		else
			makeRandomSick(agent, 0);
	}

	@Override
	public void onSpawn(Agent agent, Agent parent1, Agent parent2) {
		if ((parent1.isAlive() && isSick(parent1)) || (parent2.isAlive() && isSick(parent2)))
			makeRandomSick(agent, params.agentParams[agent.getType()].childTransmitRate);
		else
			makeRandomSick(agent, 0);
	}

	public void setParams(SimulationTimeSpace sim, DiseaseParams diseaseParams, int agentTypes) {
		this.simulation = sim;
		this.params = diseaseParams;
		sickCount = ArrayUtilities.resizeArray(sickCount, agentTypes);
	}

	private void transmitBumpOneWay(Agent bumper, Agent bumpee) {
		int tr = bumper.getType();
		int te = bumpee.getType();

		if (params.agentParams[tr].vaccinator && !isSick(bumpee) && params.agentParams[tr].canVaccinate[te]) {
			// the effectiveness is determined by the vaccinator agent type
			// maybe we can determine the effectiveness by the vaccinated agent types
			vaccinate(bumpee, params.agentParams[tr].vaccineEffectiveness);
		}

		// TODO: Modify the logics here to not remove vaccination after being healed
		if (params.agentParams[tr].healer && isSick(bumpee) && params.agentParams[tr].canHeal[te]) {
			if (simulation.getRandom().nextFloat() < params.agentParams[tr].healerEffectiveness) {
				heal(bumpee);
			}
		}

		if (!isSick(bumper))
			return;

		if (isVaccinated(bumpee)
				&& simulation.getRandom().nextFloat() < getAgentState(bumpee).vaccineEffectiveness)
			return;

		if (isSick(bumpee))
			return;

		if (params.agentParams[tr].transmitTo[te]) {
			contactTransmit(bumpee, params.agentParams[te].contactTransmitRate);
		}
	}

	public void unSick(Agent agent) {
		removeAgentState(agent);
		sickCount[agent.getType()]--;
	}

	public boolean isSick(Agent agent) {
		return hasAgentState(agent) && getAgentState(agent).sick;
	}

	public boolean isVaccinated(Agent agent) {
		return hasAgentState(agent) && getAgentState(agent).vaccinated;
	}

	// This method is for vaccinators to vaccinate agents
	public void vaccinate(Agent bumpee, float effectiveness) {
		DiseaseAgentParams agentParams = params.agentParams[bumpee.getType()];
		setAgentState(bumpee, new DiseaseState(agentParams, false, true, effectiveness));
	}

	// This method is for users to specifically vaccinate an individual agent
	public void vaccinate(Agent agent) {
		DiseaseAgentParams agentParams = params.agentParams[agent.getType()];
		setAgentState(agent, new DiseaseState(agentParams, false, true, agentParams.vaccineEffectiveness));
	}

	public void deVaccinate(Agent agent) {
		DiseaseAgentParams agentParams = params.agentParams[agent.getType()];
		setAgentState(agent, new DiseaseState(agentParams, false, false, 0.0f));
	}

	public void heal(Agent agent) {
		if (isVaccinated(agent)) {
			DiseaseAgentParams agentParams = params.agentParams[agent.getType()];
			setAgentState(agent, new DiseaseState(agentParams, false, true, agentParams.vaccineEffectiveness));
			sickCount[agent.getType()]--;
		} else {
			unSick(agent);
		}
	}

	@Override
	public void onUpdate(Agent a) {
		if (!hasAgentState(a))
			return;

		DiseaseState s = getAgentState(a);

		if (params.agentParams[a.getType()].recoveryTime == 0)
			return;

		long randomRecovery = (long) (params.agentParams[a.getType()].recoveryTime * (simulation.getRandom().nextDouble() * 0.2 + 1.0));

		if (s.sick && simulation.getTime() - s.sickStart > randomRecovery) {
			unSick(a);
		}
	}

	@Override
	protected boolean validState(DiseaseState value) {
		return value != null;
	}

}
