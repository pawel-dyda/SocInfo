package pl.edu.pja;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import pl.edu.pja.organization.Organization;
import pl.edu.pja.organization.OrganizationFactory;
import pl.edu.pja.organization.Result;
import pl.edu.pja.strategy.SimulationStrategy;

public class Main {

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.runSimulation();

    }

    private void runSimulation() throws IOException {
        Stream.of(SimulationStrategy.values()).forEach(this::shuffleLevels);
    }

    private void shuffleLevels(SimulationStrategy strategy) {
        //IntStream.rangeClosed(2, 6)
        IntStream.of(5)
        .forEach(levels -> shuffleSubordinates(levels, strategy));
    }
    
    private void shuffleSubordinates(int levels, SimulationStrategy strategy) {
        createdOutputDirectory(strategy);
        int subordinatesPerManager = 8;
        IntStream.range(0, 1000).parallel().forEach(i -> runSingleSimulation(levels, subordinatesPerManager, i, strategy));
    }

    private void createdOutputDirectory(SimulationStrategy strategy) {
        try {
            Files.createDirectories(Paths.get("work", "res", strategy.name()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runSingleSimulation(int levels, int subordinates, int i, SimulationStrategy strategy) {
        double knowledgeUsabilityRate = 0.75d;
        Organization org = OrganizationFactory.createCorporation(levels, subordinates, knowledgeUsabilityRate, strategy);
        org.start();
        List<String> results = org.getResults().stream().map(this::toCSV).collect(Collectors.toList());
        writeResults(levels, subordinates, i, strategy, results);
    }

    private String toCSV(Result r) {
        return String.format(Locale.ROOT, "%d,%.3f,%.3f", r.getWeek(), r.getWorkPerformed(), r.getReducedWorkPerformed());
    }

    private void writeResults(int levels, int subordinates, int i, SimulationStrategy strategy, List<String> results) {
        try {
            String fileName = String.format("%03d_%d_x_%d.csv", i, levels, subordinates);
            Path filePath = Paths.get("work", "res", strategy.name(), fileName);
            Files.write(filePath, results, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
