package pl.edu.pja.organization;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingByConcurrent;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import pl.edu.pja.strategy.SimulationStrategy;
import pl.edu.pja.util.PromotionUtil;

public class Corporation extends Organization {

    private static final long serialVersionUID = 3284328451738747302L;

    private static final int INITIAL_EMPLOYEE_ID = 1000;
    private static final int START_WEEK = 0;
    private static final Boolean EXTERNAL_HIRES = Boolean.FALSE;
    private static final Boolean INTERNAL_PROMOTIONS = Boolean.TRUE;

    private final EmployeeFactory _employeeFactory;
    private final PromotionUtil _promotionUtil;
    private final double _knowledgeUsabilityRate;
    private final double _orgSize;

    private Map<Employee, Set<Employee>> _subordinates = new ConcurrentHashMap<>();
    private Map<Employee, Employee> _managers = new ConcurrentHashMap<>();
    private List<Result> _results = emptyList();
    private Employee _ceo;

    public Corporation(long seed, int levels, int subordinates, double knowledgeUsabilityRate, SimulationStrategy strategy) {
        super(seed);
        _knowledgeUsabilityRate = knowledgeUsabilityRate;
        _employeeFactory = new EmployeeFactory(seed, INITIAL_EMPLOYEE_ID, strategy);
        _promotionUtil = new PromotionUtil(seed);
        _orgSize = (Math.pow(subordinates, levels) - 1) / (subordinates - 1);
        initialize(levels, subordinates);
    }

    private void initialize(int levels, int subordinatesCount) {
        _ceo = _employeeFactory.createEmployee(this, START_WEEK, true);
        Set<Employee> topLevelManagers = initializeLevel(_ceo, subordinatesCount, true);
        _subordinates.put(_ceo, topLevelManagers);
        topLevelManagers.stream().forEach(manager -> initializeSubLevels(manager, levels - 1, subordinatesCount));
    }

    private Set<Employee> initializeLevel(Employee manager, int subordinatesCount, boolean managersLevel) {
        Set<Employee> employees = generateEmployees(subordinatesCount, managersLevel);
        initializeTeam(manager, employees);
        return employees;
    }

    private Set<Employee> generateEmployees(int howMany, boolean isManager) {
        return IntStream.range(0, howMany).boxed().map(i -> _employeeFactory.createEmployee(this, START_WEEK, true))
                .collect(Collectors.toSet());
    }

    private void initializeTeam(Employee manager, Set<Employee> employees) {
        requireNonNull(manager);
        requireNonNull(employees);
        employees.stream().forEach(emp -> {
            Set<Employee> coworkers = new HashSet<>(employees);
            coworkers.remove(emp);
            _managers.put(emp, manager);
        });
    }

    private void initializeSubLevels(Employee manager, int level, int subordinatesCount) {
        // Initialize unless everything is already initialized
        if (level == 0)
            return;
        Set<Employee> directReports = initializeLevel(manager, subordinatesCount, level > 1);
        _subordinates.put(manager, directReports);
        directReports.stream().forEach(report -> initializeSubLevels(report, level - 1, subordinatesCount));
    }

    @Override
    public Set<Employee> getCoworkers(Employee emp) {
        return getManager(emp)
                .map(this::getSubordinates)
                .map(team -> team.stream()
                        .filter(e -> !e.equals(emp))
                        .collect(toSet()))
                .orElse(emptySet());
    }

    @Override
    public Set<Employee> getSubordinates(Employee emp) {
        return _subordinates.getOrDefault(emp, emptySet());
    }

    @Override
    public Optional<Employee> getManager(Employee emp) {
        return Optional.ofNullable(_managers.get(emp));
    }

    @Override
    public void start() {
        super.start();
        int weeksPerYear = 52;
        int simulationLengthInYears  = 16;
        _results = IntStream.rangeClosed(1, (weeksPerYear * simulationLengthInYears) + 1).boxed()
                .map(this::computeWeeklyResults)
                .collect(toList());
    }

    private Result computeWeeklyResults(int week) {
        updateKnowledge(week);
        performResignations(week);            
        if (isEndOfTheQuarter(week)) {
            reduceEmployees(week);
        }
        double workPerformed = _ceo.getRealWorkPerformed();
        double reducedWorkPerformed = workPerformed / _orgSize;

        return new Result(week, workPerformed, reducedWorkPerformed);
    }

    private void updateKnowledge(long step) {
        _ceo.updateKnowledge(step);
        updateSubordinatesKnowledge(_ceo, step);
    }

    private void updateSubordinatesKnowledge(Employee manager, long step) {
        getSubordinates(manager).stream().forEach(emp -> {
            emp.updateKnowledge(step);
            updateSubordinatesKnowledge(emp, step);
        });
    }

    private void performResignations(long step) {
        List<Employee> quitingTopPerformers = findQuitingTopPerformers();

        ConcurrentMap<Boolean, List<Employee>> quitingEmployeesByPromotionStatus = quitingTopPerformers.stream()
                .collect(groupingByConcurrent(this::shouldPromoteInternally));

        replaceEmployees(step, quitingEmployeesByPromotionStatus);
    }

    private List<Employee> findQuitingTopPerformers() {
        List<Employee> badManagers = findBadManagers();
        return badManagers.stream()
                .map(this::topPerformerPossiblyResigns)
                .filter(Optional::isPresent).map(Optional::get)
                .collect(toList());
    }

    private List<Employee> findBadManagers() {
        Set<Employee> allManagers = getAllManagers();
        return allManagers.stream()
                .filter(manager -> manager.getRealWorkPerformed() < 0d)
                .collect(toList());
    }

    private Set<Employee> getAllManagers() {
        Set<Employee> allManagers = new HashSet<>(_managers.values());
        return allManagers;
    }

    private Optional<Employee> topPerformerPossiblyResigns(Employee manager) {
        Optional<Employee> maybeTopPerformer = findTopPerformer(manager);
        return maybeTopPerformer.filter(tp -> _promotionUtil.topPerformerResignes());
    }

    private Optional<Employee> findTopPerformer(Employee manager) {
        Comparator<Employee> byKnowledge = (a, b) -> (int) Math.floor((a.getKnowledge() - b.getKnowledge()));
        return getSubordinates(manager).stream().max(byKnowledge);
    }

    private Boolean shouldPromoteInternally(Employee emp) {
        int hierarchyLevel = getHierarchyLevel(emp);
        return Boolean.valueOf(emp.isManager() && _promotionUtil.promoteInternally(hierarchyLevel));
    }

    private int getHierarchyLevel(Employee emp) {
        int level = 0;
        Employee current = emp;
        while (getManager(current).isPresent()) {
            level++;
            current = getManager(current).get();
        }
        return level;
    }
    
    private void replaceEmployees(long step, ConcurrentMap<Boolean, List<Employee>> employeesByReplacementStatus) {
        employeesByReplacementStatus.getOrDefault(EXTERNAL_HIRES, emptyList()).stream()
        .forEach(hireReplacement(step));
        employeesByReplacementStatus.getOrDefault(INTERNAL_PROMOTIONS, emptyList()).stream()
        .forEach(promoteInternally(step));
    }

    private Consumer<Employee> hireReplacement(long step) {
        return emp -> hireReplacement(emp, hiringWeekFor(step));
    }

    private void hireReplacement(Employee emp, int hireWeek) {
        Employee replacement = _employeeFactory.createEmployee(this, hireWeek, emp.isManager());
        replaceEmployee(emp, replacement);
    }

    private void replaceEmployee(Employee emp, Employee replacement) {
        if (emp.isManager()) {
            // set suboordinates
            Set<Employee> subordinates = getSubordinates(emp).stream().filter(s -> !s.equals(replacement))
                    .collect(toSet());
            _subordinates.put(replacement, subordinates);
            // replace manager for subordinates
            subordinates.stream().forEach(s -> _managers.put(s, replacement));
            _subordinates.remove(emp);
        }

        Optional<Employee> maybeManager = getManager(emp);
        if (maybeManager.isPresent()) {
            _managers.put(replacement, maybeManager.get());
            replaceSubordinates(maybeManager.get(), emp, replacement);
        }
    }

    private void replaceSubordinates(Employee manager, Employee quitingEmployee, Employee replacement) {
        Stream<Employee> remainingTeam = getSubordinates(manager).stream().filter(e -> !e.equals(quitingEmployee));
        Set<Employee> updatedTeam = Stream.concat(remainingTeam, Stream.of(replacement)).collect(toSet());
        _subordinates.put(manager, updatedTeam);
    }

    private int hiringWeekFor(long step) {
        return (int) step - 1;
    }

    private Consumer<Employee> promoteInternally(long step) {
        return emp -> promoteReplacementInternally(emp, hiringWeekFor(step));
    }

    private void promoteReplacementInternally(Employee emp, int hiringWeek) {
        Employee promotionCandidate = peekEmployeeForPromotion(emp);
        // Hire replacement for internally promoted candidate
        // It's not likely that anybody will want to make drastic changes to the
        // structure
        replaceEmployee(promotionCandidate, _employeeFactory.createEmployee(this, hiringWeek, promotionCandidate.isManager()));
        // update knowledge - not all of what you know could be applied one level above
        promotionCandidate.setKnowledge(promotionCandidate.getKnowledge() * _promotionUtil.getLevelUpKnowledgeApplicability());
        replaceEmployee(emp, promotionCandidate);
    }

    private Employee peekEmployeeForPromotion(Employee mgr) {
        if (_promotionUtil.shouldPromoteTopPerformer()) {
            return findTopPerformer(mgr).get();
        }

        return findBestSelfPromoter(mgr);
    }

    private Employee findBestSelfPromoter(Employee manager) {
        Set<Employee> subordinates = getSubordinates(manager);
        double averageTeamMemberWork = getAverageTeamMemberWork(subordinates);
        return subordinates.stream().max(compareByVirtualWork(averageTeamMemberWork)).get();
    }

    private double getAverageTeamMemberWork(Set<Employee> team) {
        return team.stream().map(Employee::getRealWorkPerformed).reduce(Double::sum).orElse(1d);
    }

    private Comparator<? super Employee> compareByVirtualWork(double averageTeamMemberWork) {
        return (a, b) -> (int) Math.floor(
                (a.getVirtualWorkPerformed(averageTeamMemberWork) - b.getVirtualWorkPerformed(averageTeamMemberWork)));
    }

    private boolean isEndOfTheQuarter(long step) {
        return step % 13 == 0;
    }

    private void reduceEmployees(long step) {
        Set<Employee> allManagers = getAllManagers();
        ConcurrentMap<Boolean, List<Employee>> employeesForReplacement = allManagers.stream()
                .map(this::peekEmployeeForReduction)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(groupingByConcurrent(this::shouldPromoteInternally));
        replaceEmployees(step, employeesForReplacement);
    }

    private Optional<Employee> peekEmployeeForReduction(Employee manager) {
        if (_promotionUtil.shouldReducePersonel()) {
            if (_promotionUtil.shouldReduceSelfPromoter())
                return Optional.of(manager).filter(Employee::isManager).map(this::findBestSelfPromoter);

            Set<Employee> subordinates = getSubordinates(manager);
            double averageTeamMemberWork = getAverageTeamMemberWork(subordinates);
            return subordinates.stream().min(compareByVirtualWork(averageTeamMemberWork));
        }

        return Optional.empty();
    }

    @Override
    public double getKnowledgeUsabilityRate() {
        return _knowledgeUsabilityRate;
    }

    @Override
    public List<Result> getResults() {
        return _results;
    }

}
