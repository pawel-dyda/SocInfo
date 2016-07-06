package pl.edu.pja.util;

import ec.util.MersenneTwisterFast;
import sim.util.distribution.Normal;
import sim.util.distribution.Uniform;

public class PromotionUtil {

    private static final double EXTERNAL_HIRE_PROBABILITY = 0.5d;
    private Uniform _booleanRandomGenerator;
    private Normal _knowledgeApplicabilityGenerator;

    public PromotionUtil(long seed) {
        MersenneTwisterFast twister = new MersenneTwisterFast(seed);
        _booleanRandomGenerator = new Uniform(0d, 1d, twister);
        _knowledgeApplicabilityGenerator = new Normal(0.6d, 0.2d, twister);
    }

    public boolean topPerformerResignes() {
        return _booleanRandomGenerator.nextBoolean();
    }

    public boolean promoteInternally(int hierarchyLevel) {
        if (hierarchyLevel == 0)
            throw new IllegalArgumentException("Hierarchy level must be positive non-zero!");

        double promotionProbability = 1d - EXTERNAL_HIRE_PROBABILITY / hierarchyLevel;
        return _booleanRandomGenerator.nextDoubleFromTo(0d, promotionProbability) > promotionProbability / 2;
    }

    public boolean shouldPromoteTopPerformer() {
        return _booleanRandomGenerator.nextDoubleFromTo(0d, 1d) < 0.25d;
    }

    public boolean shouldReduceSelfPromoter() {
        return _booleanRandomGenerator.nextDoubleFromTo(0d, 1d) <= 0.1d;
    }
    
    public boolean shouldReducePersonel() {
        return _booleanRandomGenerator.nextDoubleFromTo(0d, 1d) <= 1d / 60;
    }

    public double getLevelUpKnowledgeApplicability() {
        return _knowledgeApplicabilityGenerator.nextDouble();
    }
}
