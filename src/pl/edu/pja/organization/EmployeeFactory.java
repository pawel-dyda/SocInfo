package pl.edu.pja.organization;

import java.util.concurrent.atomic.AtomicInteger;

import ec.util.MersenneTwisterFast;
import sim.util.distribution.Normal;
import sim.util.distribution.Uniform;

public class EmployeeFactory {
	
	private final Uniform _sexGenerator;
	private final Normal _knowledgeGenerator; 
	private final Uniform _manSelfPromotionGenerator;
	private final Uniform _womanSelfPromotionGenerator;
	private final Normal _learningRateGenerator;
	private AtomicInteger _employeeId;

	public EmployeeFactory(long seed, int initialEmployeeId) {		
		_employeeId = new AtomicInteger(initialEmployeeId);
		MersenneTwisterFast twister = new MersenneTwisterFast(seed);
		_knowledgeGenerator = new Normal(1d, 0.3d, twister);
		_learningRateGenerator = new Normal(1d, 0.2d, twister);
		_sexGenerator = new Uniform(0d, 1d, twister);
		_manSelfPromotionGenerator = new Uniform(0d, 0.6d, twister);
		_womanSelfPromotionGenerator = new Uniform(0d, 0.4d, twister);
	}
	

	public Employee createEmployee(Organization org, int hireWeek) {
		double initialKnowledge = _knowledgeGenerator.nextDouble();
		double selfPromotion;
		if (_sexGenerator.nextBoolean())
			selfPromotion = _manSelfPromotionGenerator.nextDouble();
		else
			selfPromotion = _womanSelfPromotionGenerator.nextDouble();
		double learningRate = _learningRateGenerator.nextDouble();

		return new Worker(org, _employeeId.getAndIncrement(), hireWeek, initialKnowledge, selfPromotion, learningRate);
	}

}
