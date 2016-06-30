package pl.edu.pja.organization;

import java.util.Optional;
import java.util.Set;

import sim.engine.SimState;

public abstract class Organization extends SimState {
	
	private static final long serialVersionUID = 9048955491941212328L;
	
	public Organization(long seed) {
		super(seed);
	}

	public abstract Set<Employee> getCoworkers(Employee emp);

	public abstract Set<Employee> getSubordinates(Employee emp);

	public abstract Optional<Employee> getManager(Employee emp);

	public abstract double getKnowledgeUsabilityRate();

}
