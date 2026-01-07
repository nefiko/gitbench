package generated;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class UserService_findAll_Benchmark {

    private service.UserService instance;

    @Setup(Level.Trial)
    public void setup() {
        instance = ;
    }

    @Benchmark
    public Object benchmark() {
        return instance.findAll();
    }
}