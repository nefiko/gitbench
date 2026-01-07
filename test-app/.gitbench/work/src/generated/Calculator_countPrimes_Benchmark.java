package generated;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class Calculator_countPrimes_Benchmark {

    private service.Calculator instance;

    @Setup(Level.Trial)
    public void setup() {
        instance = new service.Calculator();
    }

    @Benchmark
    public Object benchmark() {
        return instance.countPrimes(100);
    }
}