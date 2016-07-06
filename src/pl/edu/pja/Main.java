package pl.edu.pja;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import pl.edu.pja.organization.Organization;
import pl.edu.pja.organization.OrganizationFactory;
import pl.edu.pja.organization.Result;

public class Main {

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        main.runSimulation();

    }

    private void runSimulation() throws IOException {
        Files.createDirectories(Paths.get("work", "res"));
        int levels = 4;
        int subordinates = 8;
        IntStream.range(572, 1000).parallel().forEach(i -> runSingleSimulation(levels, subordinates, i));
    }

    private void runSingleSimulation(int levels, int subordinates, int i) {
        double knowledgeUsabilityRate = 0.75d;
        Organization org = OrganizationFactory.createCorporation(levels, subordinates, knowledgeUsabilityRate);
        org.start();
        List<String> results = org.getResults().stream().map(this::toCSV).collect(Collectors.toList());
        try {
            Files.write(Paths.get("work", "res", String.format("%03d_%d_x_%d.csv", i, levels, subordinates)), results, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String toCSV(Result r) {
        return String.format(Locale.ROOT, "%d,%.3f,%.3f", r.getWeek(), r.getWorkPerformed(), r.getReducedWorkPerformed());
    }

}
