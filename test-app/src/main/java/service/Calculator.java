package service;

import org.springframework.stereotype.Component;

@Component
public class Calculator {

    // OPTIMIZED - memoization with iterative approach (faster)
    public long fibonacci(int n) {
        if (n <= 1) return n;
        long prev = 0, curr = 1;
        for (int i = 2; i <= n; i++) {
            long next = prev + curr;
            prev = curr;
            curr = next;
        }
        return curr;
    }

    // SLOWED DOWN - added unnecessary loop (slower)
    public long factorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
            // Simulate extra work
            for (int j = 0; j < 100; j++) {
                result = result + 1 - 1;
            }
        }
        return result;
    }

    // SAME
    public int sumArray(int[] numbers) {
        int sum = 0;
        for (int num : numbers) {
            sum += num;
        }
        return sum;
    }

    // SLOWED DOWN - check all numbers instead of sqrt (slower)
    public boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i < n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }

    // SAME (but will be slower because isPrime is slower)
    public int countPrimes(int max) {
        int count = 0;
        for (int i = 2; i <= max; i++) {
            if (isPrime(i)) {
                count++;
            }
        }
        return count;
    }
}