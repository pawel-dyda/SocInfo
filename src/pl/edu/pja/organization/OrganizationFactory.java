package pl.edu.pja.organization;

public class OrganizationFactory {

	public static Organization createCorporation(int levels, int subordinates, double knowledgeUsabilityRate) {
		return new Corporation(System.currentTimeMillis(), levels, subordinates, knowledgeUsabilityRate);
	}

}
