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

import pl.edu.pja.util.PromotionUtil;

public class Corporation extends Organization {

    private static final long serialVersionUID = 3284328451738747302L;

    private static final int INITIAL_EMPLOYEE_ID = 1000;
    private static final int START_WEEK = 0;
    private static final Boolean EXTERNAL_HIRES = Boolean.FALSE;

    private final EmployeeFactory _employeeFactory;
    private final PromotionUtil _promotionUtil;
    private final double _knowledgeUsabilityRate;

    // private Map<Employee, Set<Employee>> _coworkers = new
    // ConcurrentHashMap<>();
    private Map<Employee, Set<Employee>> _subordinates = new ConcurrentHashMap<>();
    private Map<Employee, Employee> _managers = new ConcurrentHashMap<>();
    private Employee _ceo;

    public Corporation(long seed, int levels, int subordinates, double knowledgeUsabilityRate) {
        super(seed);
        _knowledgeUsabilityRate = knowledgeUsabilityRate;
        _employeeFactory = new EmployeeFactory(seed, INITIAL_EMPLOYEE_ID);
        _promotionUtil = new PromotionUtil(seed);
        initialize(levels, subordinates);
    }

    private void initialize(int levels, int subordinatesCount) {
        _ceo = _employeeFactory.createEmployee(this, START_WEEK);
        Set<Employee> topLevelManagers = initializeLevel(_ceo, subordinatesCount);
        _subordinates.put(_ceo, topLevelManagers);
        topLevelManagers.stream().forEach(manager -> initializeSubLevels(manager, levels - 1, subordinatesCount));
    }

    private Set<Employee> initializeLevel(Employee manager, int subordinatesCount) {
        Set<Employee> employees = generateEmployees(subordinatesCount);
        initializeTeam(manager, employees);
        return employees;
    }

    private Set<Employee> generateEmployees(int howMany) {
        return IntStream.range(0, howMany).boxed().map(i -> _employeeFactory.createEmployee(this, START_WEEK))
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
        Set<Employee> directReports = initializeLevel(manager, subordinatesCount);
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
        System.out.println("start");
        for (long step = 1; step < 52; step++) {
            System.out.println("Step: " + step);
            updateKnowledge(step);
            performResignations(step);
            System.out.println(_ceo.getRealWorkPerformed());
        }
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
        System.out.println("Resignations.");
        List<Employee> quitingTopPerformers = findQuitingTopPerformers();

        ConcurrentMap<Boolean, List<Employee>> quitingEmployeesByPromotionStatus = quitingTopPerformers.stream()
                .collect(groupingByConcurrent(this::shouldPromoteInternally));

        quitingEmployeesByPromotionStatus.getOrDefault(EXTERNAL_HIRES, emptyList()).stream()
                .forEach(hireReplacement(step));
        quitingEmployeesByPromotionStatus.getOrDefault(Boolean.TRUE, emptyList()).stream()
                .forEach(promoteInternally(step));
    }

    private List<Employee> findQuitingTopPerformers() {
        List<Employee> badManagers = findBadManagers();
        return badManagers.stream()
                .map(this::topPerformerPossiblyResigns)
                .filter(Optional::isPresent).map(Optional::get)
                .collect(toList());
    }

    private List<Employee> findBadManagers() {
        Set<Employee> allManagers = new HashSet<>(_managers.values());
        return allManagers.stream()
                .filter(manager -> manager.getRealWorkPerformed() < 0d)
                .collect(toList());
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

    private Consumer<Employee> hireReplacement(long step) {
        return emp -> hireReplacement(emp, hiringWeekFor(step));
    }

    private void hireReplacement(Employee emp, int hireWeek) {
        Employee replacement = _employeeFactory.createEmployee(this, hireWeek, computeInitialKnowledge(emp));
        replaceEmployee(emp, replacement);
    }

    private double computeInitialKnowledge(Employee emp) {
        return getManager(emp).flatMap(mgr -> {
            Set<Employee> team = getSubordinates(mgr);
            Optional<Double> maybeSumOfTeamKnowledge = team.stream().map(Employee::getKnowledge).reduce(Double::sum);
            return maybeSumOfTeamKnowledge.map(sum -> sum / team.size());
        }).orElse(1d);
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
        replaceEmployee(promotionCandidate, _employeeFactory.createEmployee(this, hiringWeek));
        replaceEmployee(emp, promotionCandidate);
    }

    private Employee peekEmployeeForPromotion(Employee mgr) {
        if (_promotionUtil.shouldPromoteTopPerformer()) {
            return findTopPerformer(mgr).get();
        }

        return findBestSelfPromoter(mgr);
    }

    private Employee findBestSelfPromoter(Employee manager) {
        Comparator<Employee> byVirtualWork = (a,
                b) -> (int) Math.floor((a.getVirtualWorkPerformed() - b.getVirtualWorkPerformed()));
        return getSubordinates(manager).stream().max(byVirtualWork).get();
    }

    @Override
    public double getKnowledgeUsabilityRate() {
        return _knowledgeUsabilityRate;
    }

}
