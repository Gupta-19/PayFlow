package com.ketan.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class PaymentConcurrencyTest extends ApiTestBase {

    /** Waits for every future against one shared deadline, so the whole batch must finish within it. */
    private static List<Integer> awaitAll(List<Future<Integer>> results, long startNanos, long timeoutMs)
            throws Exception {
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) {
            long remainingMs = timeoutMs - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            statuses.add(f.get(Math.max(remainingMs, 1), TimeUnit.MILLISECONDS));
        }
        return statuses;
    }

    @Test
    void twentyParallelPayments_neverOverdraw() throws Exception {
        Account a = newAccount();
        Account b = newAccount();
        topUp(a, 10_000, "topup-" + UUID.randomUUID());   // Rs 100

        List<Integer> statuses;
        try (ExecutorService pool = Executors.newFixedThreadPool(20)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String key = "pay-" + i;
                results.add(pool.submit(() -> {
                    start.await();                              // all 20 threads fire together
                    return pay(a, b.walletId(), 1_000, key);    // Rs 10 each
                }));
            }
            long t0 = System.nanoTime();
            start.countDown();
            statuses = awaitAll(results, t0, 30_000);
        }

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(10);
        assertThat(statuses).filteredOn(s -> s == 422).hasSize(10);
        assertThat(balance(a)).isZero();
        assertThat(balance(b)).isEqualTo(10_000);
    }

    @Test
    void crossTransfers_noDeadlock() throws Exception {
        Account a = newAccount();
        Account b = newAccount();
        topUp(a, 100_000, "topup-" + UUID.randomUUID());
        topUp(b, 100_000, "topup-" + UUID.randomUUID());

        List<Integer> statuses;
        try (ExecutorService pool = Executors.newFixedThreadPool(20)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                String round = UUID.randomUUID().toString();
                results.add(pool.submit(() -> {
                    start.await();
                    return pay(a, b.walletId(), 100, "ab-" + round);
                }));
                results.add(pool.submit(() -> {
                    start.await();
                    return pay(b, a.walletId(), 100, "ba-" + round);
                }));
            }
            long t0 = System.nanoTime();
            start.countDown();
            statuses = awaitAll(results, t0, 30_000);   // all 100 within 30 s total
        }

        assertThat(statuses).hasSize(100).allMatch(s -> s == 201);
        assertThat(balance(a)).isEqualTo(100_000);
        assertThat(balance(b)).isEqualTo(100_000);
    }
}
