package pl.edu.pja.organization;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import pl.edu.pja.util.PromotionUtil;

public class Corporation extends Organization {

	private static final long serialVersionUID = 3284328451738747302L;

	private static final int INITIAL_EMPLOYEE_ID = 1000;
	private static final int START_WEEK = 0;
	
	private final EmployeeFactory _employeeFactory;
	private final PromotionUtil _promotionUtil;
	private final double _knowledgeUsabilityRate;

	private Map<Employee, Set<Employee>> _coworkers = new ConcurrentHashMap<>();
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
		_coworkers.put(_ceo, emptySet());
		_subordinates.put(_ceo, topLevelManagers);
		topLevelManagers.stream()
			.forEach(manager -> initializeSubLevels(manager, levels - 1, subordinatesCount));			
	}
	
	private Set<Employee> initializeLevel(Employee manager, int subordinatesCount) {
		Set<Employee> employees = generateEmployees(subordinatesCount);
		initializeTeam(manager, employees);
		return employees;
	}

	private Set<Employee> generateEmployees(int howMany) {
		return IntStream.range(0, howMany)
				.boxed()
				.map(i -> _employeeFactory.createEmployee(this, START_WEEK))
				.collect(Collectors.toSet());
	}

	private void initializeTeam(Employee manager, Set<Employee> employees) {
		requireNonNull(manager);
		requireNonNull(employees);
		employees.stream().forEach(emp -> {			
			Set<Employee> coworkers = new HashSet<>(employees);
			coworkers.remove(emp);
			_coworkers.put(emp, coworkers);
			_managers.put(emp, manager);
		});
	}
	
	private void initializeSubLevels(Employee manager, int level, int subordinatesCount) {
		// Initialize unless everything is already initialized
		if (level == 0)
			return;
		Set<Employee> directReports = initializeLevel(manager, subordinatesCount);
		_subordinates.put(manager, directReports);
		directReports.stream()
			.forEach(report -> initializeSubLevels(report, level - 1, subordinatesCount));
	}

	@Override
	public Set<Employee> getCoworkers(Employee emp) {
		return _coworkers.getOrDefault(emp, new HashSet<>());
	}

	@Override
	public Set<Employee> getSubordinates(Employee emp) {
		return _subordinates.getOrDefault(emp, new HashSet<>());
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
//			performResignations(step);
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
		Set<Employee> allManagers = new HashSet<>(_managers.values());
		List<Employee> badManagers = allManagers.stream()
				.filter(manager -> manager.getRealWorkPerformed() < 0d)
				.collect(toList());
		badManagers.stream().forEach(mgr -> topPerformerPossiblyResignes(mgr, step));
	}

	private void topPerformerPossiblyResignes(Employee manager, long step) {
		Optional<Employee> maybeTopPerformer = findTopPerformer(manager);
		if (maybeTopPerformer.isPresent() && _promotionUtil.topPerformerResignes()) {
			Employee topPerformer = maybeTopPerformer.get();
			int hireWeek = (int) step - 1;
			hireOrPromoteReplacement(topPerformer, hireWeek);
			removeEmployee(topPerformer);
		}
	}

	private Optional<Employee> findTopPerformer(Employee manager) {
		Comparator<Employee> byKnowledge = (a,b) -> (int) Math.floor((a.getKnowledge() - b.getKnowledge()));
		return getSubordinates(manager).stream().max(byKnowledge);
	}

	private void hireOrPromoteReplacement(Employee emp, int hireWeek) {
		Employee replacement;
		if (emp.isManager() && shouldPromoteInternally(emp)) {
			replacement = peekEmployeeForPromotion(emp);
			hireOrPromoteReplacement(replacement, hireWeek);
		} else {
			replacement = _employeeFactory.createEmployee(this, hireWeek);
		}
		updateOrganizationStructure(emp, replacement);
	}

	public boolean shouldPromoteInternally(Employee emp) {
		int hierarchyLevel = getHierarchyLevel(emp);
		return _promotionUtil.promoteInternally(hierarchyLevel);
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

	private Employee peekEmployeeForPromotion(Employee mgr) {
		if (_promotionUtil.shouldPromoteTopPerformer()) {
			return findTopPerformer(mgr).get();
		}

		return findBestSelfPromoter(mgr);
	}
	
	private Employee findBestSelfPromoter(Employee manager) {
		Comparator<Employee> byVirtualWork = (a,b) -> (int) Math.floor((a.getVirtualWorkPerformed() - b.getVirtualWorkPerformed()));
		return getSubordinates(manager).stream().max(byVirtualWork).get();
	}

	private void updateOrganizationStructure(Employee emp, Employee replacement) {
		if (emp.isManager()) {
			_subordinates.put(replacement, getSubordinates(emp));
		}
		if (getManager(emp).isPresent()) {
			_managers.put(replacement, getManager(emp).get());
		}
		getCoworkers(emp).stream().forEach(coworker -> updateCoworkers(coworker, emp, replacement));
	}

	private void updateCoworkers(Employee emp, Employee replaced, Employee replacement) {
		Set<Employee> coworkers = getCoworkers(emp);
		System.out.println(coworkers.size());
		if (coworkers.size() == 0) {
			System.out.println(coworkers.getClass());
			System.out.println(_ceo);
			System.out.println(emp);
			Worker w = (Worker) emp;
			System.out.println(replaced);
			System.out.println(replacement);
		}
		coworkers.remove(replaced);
		coworkers.add(replacement);
		_coworkers.put(emp, coworkers);
	}
	
	private void removeEmployee(Employee emp) {
		if (getManager(emp).isPresent()) {
			Employee formerManager = getManager(emp).get();
			Set<Employee> formerMgrSubordinates = getSubordinates(formerManager);
			formerMgrSubordinates.remove(emp);
			_subordinates.put(formerManager, formerMgrSubordinates);
		}
		_managers.remove(emp);
	}

	@Override
	public double getKnowledgeUsabilityRate() {
		return _knowledgeUsabilityRate;
	}

}
