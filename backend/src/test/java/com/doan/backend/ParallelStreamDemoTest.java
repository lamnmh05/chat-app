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
                .sequential() // Lệnh gọi cuối cùng quyết định tất cả
                .isParallel();
        System.out.println("Trạng thái chốt (do gọi .sequential() cuối cùng): " + finalState);
    }

    @Test
    public void demo2_PerformanceBenchmark() {
        System.out.println("\n=== DEMO 2: BENCHMARK HIỆU NĂNG (ARRAYLIST VS LINKEDLIST) ===");
        int N = 1_000_000;
        List<Integer> arrayList = new ArrayList<>(N);
        List<Integer> linkedList = new LinkedList<>();

        for (int i = 0; i < N; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        long start = System.currentTimeMillis();
        arrayList.stream().filter(this::isPrime).count();
        System.out.println("1. Sequential (Tuần tự):         " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        arrayList.parallelStream().filter(this::isPrime).count();
        System.out.println("2. Parallel (ArrayList - Tốt):   " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        linkedList.parallelStream().filter(this::isPrime).count();
        System.out.println("3. Parallel (LinkedList - Kém):  " + (System.currentTimeMillis() - start) + "ms");
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

    // Hàm phụ trợ tính số nguyên tố để tạo tải (load) cho CPU ở Demo 2
    private boolean isPrime(int number) {
        if (number <= 1) return false;
        for (int i = 2; i <= Math.sqrt(number); i++) {
            if (number % i == 0) return false;
        }
        return true;
    }
}