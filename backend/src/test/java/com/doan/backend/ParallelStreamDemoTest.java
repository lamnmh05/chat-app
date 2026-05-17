package com.doan.backend;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ParallelStreamDemoTest {

    @Test
    public void demo1_ActivationAndState() {
        System.out.println("=== DEMO 1: KÍCH HOẠT VÀ TRẠNG THÁI STREAM ===");
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

        boolean isParallel1 = numbers.parallelStream().isParallel();
        System.out.println("Tạo từ Collection (.parallelStream()): " + isParallel1);

        boolean isParallel2 = IntStream.range(1, 100).parallel().isParallel();
        System.out.println("Tạo từ Stream có sẵn (.parallel()): " + isParallel2);

        boolean finalState = numbers.stream()
                .parallel()
                .filter(n -> n % 2 == 0)
                .sequential()
                .isParallel();
        System.out.println("Trạng thái chốt (do gọi .sequential() cuối cùng): " + finalState);
    }

    @Test
    public void demo2_RealWorldPerspectives() {
        System.out.println("\n=== DEMO 2: GÓC NHÌN ĐA CHIỀU TRONG THẾ GIỚI THỰC ===");
        int N = 5_000_000;
        List<Integer> arrayList = new ArrayList<>(N);
        List<Integer> linkedList = new LinkedList<>();

        for (int i = 1; i <= N; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        arrayList.stream().limit(10_000).mapToDouble(this::collatzConjecture).sum();
        arrayList.parallelStream().limit(10_000).mapToDouble(this::collatzConjecture).sum();

        System.out.println("\n[SCENARIO 1] Tính toán nhẹ (Toán cơ bản O(1)):");
        measure("Sequential", () -> arrayList.stream().mapToDouble(n -> n * 2.5 / 1.3).sum());
        measure("Parallel (ArrayList)", () -> arrayList.parallelStream().mapToDouble(n -> n * 2.5 / 1.3).sum());
        measure("Parallel (LinkedList)", () -> linkedList.parallelStream().mapToDouble(n -> n * 2.5 / 1.3).sum());

        System.out.println("\n[SCENARIO 2] Tải không đồng đều (Thuật toán Collatz Conjecture):");
        measure("Sequential", () -> arrayList.stream().mapToLong(this::collatzConjecture).sum());
        measure("Parallel (ArrayList)", () -> arrayList.parallelStream().mapToLong(this::collatzConjecture).sum());
        measure("Parallel (LinkedList)", () -> linkedList.parallelStream().mapToLong(this::collatzConjecture).sum());

        System.out.println("\n[SCENARIO 3] Tải siêu nặng (Giả lập Hash/Crypto):");
        // Rút số lượng xuống để test không chạy quá lâu (test trên 200,000 phần tử đầu)
        measure("Sequential", () -> arrayList.stream().limit(200_000).mapToDouble(this::heavyHashSim).sum());
        measure("Parallel (ArrayList)", () -> arrayList.parallelStream().limit(200_000).mapToDouble(this::heavyHashSim).sum());
        measure("Parallel (LinkedList)", () -> linkedList.parallelStream().limit(200_000).mapToDouble(this::heavyHashSim).sum());
    }
    private long collatzConjecture(long n) {
        long count = 0;
        while (n > 1) {
            n = (n % 2 == 0) ? (n / 2) : (3 * n + 1);
            count++;
        }
        return count;
    }

    // Thuật toán giả lập Crypto: Phức tạp, nặng nề, nhưng đồng đều cho mọi phần tử
    private double heavyHashSim(int n) {
        double result = n;
        for (int i = 0; i < 50; i++) {
            result = Math.sin(result) * Math.cos(result) + Math.tan(result);
        }
        return result;
    }

    private void measure(String label, Runnable task) {
        long start = System.currentTimeMillis();
        task.run();
        long end = System.currentTimeMillis();
        System.out.printf("%-25s : %d ms\n", label, (end - start));
    }


    @Test
    public void demo3_SharedMutableStateTrap() {
        System.out.println("\n=== DEMO 3: BẪY CHIA SẺ TRẠNG THÁI (STATELESS) ===");
        List<Integer> numbers = IntStream.range(0, 1000).boxed().collect(Collectors.toList());
        List<Integer> resultList = new ArrayList<>(); // Shared mutable state

        // CÁCH SAI
        numbers.parallelStream().forEach(resultList::add);
        System.out.println("Số lượng phần tử (Cách sai - Data Loss): " + resultList.size() + " (Kỳ vọng: 1000)");

        // CÁCH ĐÚNG
        List<Integer> correctList = numbers.parallelStream().collect(Collectors.toList());
        System.out.println("Số lượng phần tử (Cách đúng - Stateless): " + correctList.size() + " (Kỳ vọng: 1000)");
    }

    @Test
    public void demo4_OrderingAndStatefulOperations() {
        System.out.println("\n=== DEMO 4: THỨ TỰ VÀ STATEFUL OPERATIONS ===");
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        System.out.print("Thứ tự in ra (Không đảm bảo): ");
        numbers.parallelStream().forEach(n -> System.out.print(n + " "));
        System.out.println();

        long start = System.currentTimeMillis();
        IntStream.range(0, 10_000_000).parallel().filter(n -> n > 5_000_000).findFirst();
        System.out.println("Thời gian dùng findFirst() (Chậm vì ép giữ order): " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        IntStream.range(0, 10_000_000).parallel().filter(n -> n > 5_000_000).findAny();
        System.out.println("Thời gian dùng findAny()   (Nhanh vì bỏ qua order): " + (System.currentTimeMillis() - start) + "ms");
    }

    @Test
    public void demo5_CommonPoolTrap() {
        System.out.println("\n=== DEMO 5: BẪY COMMON FORK-JOIN POOL (I/O BOUND) ===");
        List<Integer> tasks = IntStream.range(0, 20).boxed().collect(Collectors.toList());

        long start = System.currentTimeMillis();
        tasks.parallelStream().forEach(task -> {
            try {
                Thread.sleep(100); // Giả lập I/O block (VD: gọi API, đọc file mất 100ms)
            } catch (InterruptedException e) {}
        });
        long totalTime = System.currentTimeMillis() - start;
        System.out.println("Tổng thời gian chạy 20 task (mỗi task 100ms): " + totalTime + "ms");
        System.out.println("-> Giải thích: Không phải 100ms vì số luồng Common Pool bị giới hạn bởi số Core CPU!");
    }

    // Hàm phụ trợ tính số nguyên tố để tạo tải (load) cho CPU
    private boolean isPrime(int number) {
        if (number <= 1) return false;
        for (int i = 2; i <= Math.sqrt(number); i++) {
            if (number % i == 0) return false;
        }
        return true;
    }
}