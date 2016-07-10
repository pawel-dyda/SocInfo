package pl.edu.pja.organization;

import java.util.concurrent.atomic.AtomicInteger;

import ec.util.MersenneTwisterFast;
import pl.edu.pja.strategy.SimulationStrategy;
import sim.util.distribution.Normal;
import sim.util.distribution.Uniform;

public class EmployeeFactory {

    private static final boolean WOMAN = false;
    private static final boolean MAN = true;

    private final Uniform _sexGenerator;
    private final Normal _knowledgeGenerator;
    private final Uniform _manSelfPromotionGenerator;
    private final Uniform _womanSelfPromotionGenerator;
    private final Normal _learningRateGenerator;
    private AtomicInteger _employeeId;
    private SimulationStrategy _simulationStrategy;

    public EmployeeFactory(long seed, int initialEmployeeId, SimulationStrategy strategy) {
        _employeeId = new AtomicInteger(initialEmployeeId);
        _simulationStrategy = strategy;
        MersenneTwisterFast twister = new MersenneTwisterFast(seed);
        _knowledgeGenerator = new Normal(1d, 0.3d, twister);
        _learningRateGenerator = new Normal(1d, 0.2d, twister);
        _sexGenerator = new Uniform(0d, 1d, twister);
        _manSelfPromotionGenerator = new Uniform(0d, 0.6d, twister);
        _womanSelfPromotionGenerator = new Uniform(0d, 0.4d, twister);
    }

    public Employee createEmployee(Organization org, int hireWeek, boolean isManager) {
        return createEmployee(org, hireWeek, isManager, _knowledgeGenerator.nextDouble());
    }

    public Employee createEmployee(Organization org, int hireWeek, boolean isManager, double initialKnowledge) {
        double selfPromotion = getSelfPromotionRate(isManager);
        double learningRate = _learningRateGenerator.nextDouble();

        return new Worker(org, _employeeId.getAndIncrement(), hireWeek, initialKnowledge, selfPromotion, learningRate);
    }

    private double getSelfPromotionRate(boolean isManager) {
        boolean sex = getAgentSex(isManager);
        Uniform selfPromotionGenerator = getSelfPromotionGenerator(sex);
        return selfPromotionGenerator.nextDouble();
    }

    private boolean getAgentSex(boolean isManager) {
        if (isManager)
            return getSexBasedOnStrategy();
        return _sexGenerator.nextBoolean();
    }

    private boolean getSexBasedOnStrategy() {
        switch (_simulationStrategy) {
        case NO_GENDER_QUOTA:
            return _sexGenerator.nextBoolean();
        case NO_WOMEN_MANAGERS:
            return MAN;
        case FIFTY_PERCENT_WOMEN_MANAGERS:
            return _sexGenerator.nextDouble() <= 0.5d;
        case SEVENTY_FIVE_PERCENT_WOMEN_MANAGERS:
            return _sexGenerator.nextDouble() <= 0.75d;
        case ONE_HUNDRED_PERCENT_WOMEN_MANAGERS:
            return WOMAN;
        default:
            throw new IllegalArgumentException("Unknown simulation strategy: " + _simulationStrategy.name());
        }
    }

    private Uniform getSelfPromotionGenerator(boolean isMan) {
        if (isMan)
            return _manSelfPromotionGenerator;
        return _womanSelfPromotionGenerator;
    }

}
