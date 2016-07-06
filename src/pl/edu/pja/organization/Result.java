package pl.edu.pja.organization;

public class Result {

    private final int _week;
    private final double _workPerformed;
    private final double _reducedWorkPerformed;
    
    public Result(int week, double workPerformed, double reducedWorkPerformed) {
        _week = week;
        _workPerformed = workPerformed;
        _reducedWorkPerformed = reducedWorkPerformed;
    }

    public int getWeek() {
        return _week;
    }

    public double getWorkPerformed() {
        return _workPerformed;
    }

    public double getReducedWorkPerformed() {
        return _reducedWorkPerformed;
    }

}
