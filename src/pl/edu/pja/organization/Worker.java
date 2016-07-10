package pl.edu.pja.organization;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.Set;

public class Worker implements Employee {

    private static final double KNOWLEDGE_AQUISITION_RATE = 0.037d;

    private final Organization _organization;
    private final int _employeeId;
    private final int _hireWeek;
    private final double _initialKnowledge;
    private double _knowledge;
    private double _selfPromotion;
    private double _learningRate;

    public Worker(Organization organization, int employeeId, int hireWeek, double initialKnowledge,
            double selfPromotion,
            double learningRate) {
        _hireWeek = hireWeek;
        _organization = organization;
        _employeeId = employeeId;
        _initialKnowledge = initialKnowledge;
        _knowledge = initialKnowledge;
        _selfPromotion = selfPromotion;
        _learningRate = learningRate;

    }

    @Override
    public void updateKnowledge(long step) {
        double coworkersQuant = talkToCoworkers();
        double managerQuant = talkToManager();
        double reducedLearningRate = _learningRate * KNOWLEDGE_AQUISITION_RATE / (step - _hireWeek);
        _knowledge = _knowledge + reducedLearningRate * (_knowledge + coworkersQuant + managerQuant);
    }

    private double talkToCoworkers() {
        Set<Employee> coworkers = requireNonNull(_organization.getCoworkers(this));
        Optional<Double> maybeCoworkersUpdate = coworkers.stream()
                .parallel()
                .map(this::computeCoworkersKnowledgeUpdate)
                .reduce(Double::sum);
        return maybeCoworkersUpdate.orElse(Double.valueOf(0d)).doubleValue();
    }

    private Double computeCoworkersKnowledgeUpdate(Employee coworker) {
        if (coworker instanceof Worker) {
            Worker other = (Worker) coworker;
            double attitude = _selfPromotion + other._selfPromotion;
            double update = 0.8d - 1.33d * attitude * other._knowledge;

            return Double.valueOf(update);
        }

        return Double.valueOf(0d);
    }

    private double talkToManager() {
        Optional<Employee> maybeManager = _organization.getManager(this);
        // make sure there is manager (CEO does not have one)
        Optional<Double> maybeUpdate = maybeManager
                .map(this::computeManagerUpdate);
        return maybeUpdate.orElse(Double.valueOf(0d)).doubleValue();
    }

    private Double computeManagerUpdate(Employee manager) {
        if (manager instanceof Worker) {
            Worker mgr = (Worker) manager;
            double mgrAttitude = 1 - mgr._selfPromotion;
            return Double.valueOf(mgrAttitude * mgr._knowledge);
        }

        return Double.valueOf(0d);
    }

    public double getInitialKnowledge() {
        return _initialKnowledge;
    }

    @Override
    public double getRealWorkPerformed() {
        if (isManager()) {
            Set<Employee> subordinates = _organization.getSubordinates(this);
            Optional<Double> maybeSubordinatesWork = subordinates.stream()
                    .map(Employee::getRealWorkPerformed)
                    .reduce(Double::sum);
            return getSelfRealWork() * maybeSubordinatesWork.orElse(Double.valueOf(0d)).doubleValue();
        }

        return getSelfRealWork();
    }

    @Override
    public boolean isManager() {
        return _organization.getSubordinates(this).size() > 0;
    }

    private double getSelfRealWork() {
        return _knowledge * _organization.getKnowledgeUsabilityRate();
    }

    @Override
    public double getVirtualWorkPerformed(double teamAverage) {
        return _selfPromotion + getRealWorkPerformed() / teamAverage;
    }

    @Override
    public double getKnowledge() {
        return _knowledge;
    }

    @Override
    public void setKnowledge(double knowledge) {
        _knowledge = knowledge;
    }

    @Override
    public int hashCode() {
        return _employeeId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Worker) {
            Worker other = (Worker) obj;
            return _employeeId == other._employeeId;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Employee: " + _employeeId + ", hire week: " + _hireWeek;
    }

}
