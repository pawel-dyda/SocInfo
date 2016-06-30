package pl.edu.pja;

import pl.edu.pja.organization.Organization;
import pl.edu.pja.organization.OrganizationFactory;

public class Main {

	public static void main(String[] args) {
		Organization org = OrganizationFactory.createCorporation(5, 8, 0.75d);
		org.start();

	}

}
