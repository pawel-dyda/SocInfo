package pl.edu.pja.organization;

import pl.edu.pja.strategy.SimulationStrategy;

public class OrganizationFactory {

	public static Organization createCorporation(int levels, int subordinates, double knowledgeUsabilityRate, SimulationStrategy strategy) {
		return new Corporation(System.currentTimeMillis(), levels, subordinates, knowledgeUsabilityRate, strategy);
	}

}
