package pl.edu.pja.organization;

public interface Employee {

	void updateKnowledge(long step);

	double getRealWorkPerformed();

	double getVirtualWorkPerformed(double teamAverage);

	double getKnowledge();

	boolean isManager();

    void setKnowledge(double knowledge);

}
