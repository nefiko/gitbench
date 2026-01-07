package generated;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class Calculator_sumArray_Benchmark {

    private service.Calculator instance;

    @Setup(Level.Trial)
    public void setup() {
        instance = new service.Calculator();
    }

    @Benchmark
    public Object benchmark() {
        return instance.sumArray(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
    }
}