package pl.edu.pja.organization;

public interface Employee {

	void updateKnowledge(long step);

	double getRealWorkPerformed();

	double getVirtualWorkPerformed();

	double getKnowledge();

	boolean isManager();

}
