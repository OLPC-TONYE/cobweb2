package org.cobweb.cobweb2.plugins.disease;

import java.util.Arrays;

import org.cobweb.cobweb2.core.AgentFoodCountable;
import org.cobweb.cobweb2.core.NullPhenotype;
import org.cobweb.cobweb2.core.Phenotype;
import org.cobweb.cobweb2.plugins.ResizableParam;
import org.cobweb.io.ConfDisplayName;
import org.cobweb.io.ConfList;
import org.cobweb.io.ConfXMLTag;


public class DiseaseAgentParams implements ResizableParam {

	/**
	 * Fraction of initially infected agents.
	 */
	@ConfXMLTag("initialInfection")
	@ConfDisplayName("Initially infected fraction")
	public float initialInfection = 0;

	/**
	 * Fraction of initially vaccinated agents.
	 */
	@ConfXMLTag("initialVaccination")
	@ConfDisplayName("Initially vaccinated fraction")
	public float initialVaccination = 0;

	/**
	 * Chance this agent will get a disease from contact with an infected agent.
	 */
	@ConfXMLTag("contactTransmitRate")
	@ConfDisplayName("Contact transmission rate")
	public float contactTransmitRate = 0.5f;

	/**
	 * Chance a child of an infected agent will be infected.
	 */
	@ConfXMLTag("childTransmitRate")
	@ConfDisplayName("Child transmission rate")
	public float childTransmitRate = 0.9f;

	/**
	 * Which parameter is affected by the disease.
	 */
	@ConfXMLTag("parameter")
	@ConfDisplayName("Parameter")
	public Phenotype param = new NullPhenotype();

	/**
	 * The factor the parameter is multiplied by when the agent is infected.
	 */
	@ConfXMLTag("factor")
	@ConfDisplayName("Factor")
	public float factor = 2;


	@ConfXMLTag("vaccinator")
	@ConfDisplayName("Vaccinator")
	public boolean vaccinator = false;

	@ConfXMLTag("vaccineEffectiveness")
	@ConfDisplayName("Vaccine Effectiveness")
	public float vaccineEffectiveness = 1.0f;

	@ConfXMLTag("healer")
	@ConfDisplayName("Healer")
	public boolean healer = false;

	@ConfXMLTag("healerEffectiveness")
	@ConfDisplayName("Healing Effectiveness")
	public float healerEffectiveness = 1.0f;

	@ConfXMLTag("recoveryTime")
	@ConfDisplayName("Recovery time")
	public int recoveryTime = 0;

	/**
	 * Agent types this agent type can transmit the disease to.
	 */
	@ConfDisplayName("Transmit to")
	@ConfXMLTag("transmitTo")
	@ConfList(indexName = "Agent", startAtOne = true)
	public boolean[] transmitTo = new boolean[0];

	/**
	 * Agent types this agent type can heal.
	 */
	@ConfDisplayName("Heal")
	@ConfXMLTag("heal")
	@ConfList(indexName = "Agent", startAtOne = true)
	public boolean[] canHeal = new boolean[0];

	/**
	 * Agent types this agent type can vaccinate.
	 */
	@ConfDisplayName("Vaccinate")
	@ConfXMLTag("vaccinate")
	@ConfList(indexName = "Agent", startAtOne = true)
	public boolean[] canVaccinate = new boolean[0];

	@Deprecated // for reflection use only!
	public DiseaseAgentParams(){
	}

	public DiseaseAgentParams(AgentFoodCountable size) {
		resize(size);
	}

	@Override
	public void resize(AgentFoodCountable size) {
		boolean[] n = Arrays.copyOf(transmitTo, size.getAgentTypes());
		this.transmitTo = n;
		boolean[] m = Arrays.copyOf(canHeal, size.getAgentTypes());
		this.canHeal = m;
		boolean[] o = Arrays.copyOf(canVaccinate, size.getAgentTypes());
		this.canVaccinate = o;
	}

	private static final long serialVersionUID = 2L;
}
