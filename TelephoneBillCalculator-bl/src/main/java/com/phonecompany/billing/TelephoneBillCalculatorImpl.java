package com.phonecompany.billing;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TelephoneBillCalculatorImpl implements TelephoneBillCalculator {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private static final int discountedTime = 5;

    private static final BigDecimal baseRate = new BigDecimal("1.00");
    private static final BigDecimal offRate = new BigDecimal("0.50");
    private static final BigDecimal discRate = new BigDecimal("0.20");

    private static final LocalTime baseRateStart = LocalTime.of(8, 0, 0);
    private static final LocalTime baseRateEnd = LocalTime.of(16, 0, 0);


    private Long mostCalledNumber = null;

    public BigDecimal calculate(String phoneLog) throws Exception {
        BigDecimal amount = BigDecimal.ZERO;
        final List<CallRecord> records = parseLog(phoneLog);
        if (!records.isEmpty()) {
            mostCalledNumber = getMostCalledNumber(records);
            for (CallRecord r : records) {
                amount = amount.add(getAmount(r));
            }
        }
        return amount;
    }

    /*
    - Minutová sazba uvnitř časového intervalu od 8:00 do 16:00 je zpoplatněna 1 Kč za každou
      započatou minutu. Mimo uvedený interval platí snížená sazba 0,50 Kč za každou započatou
      minutu. Pro každou minutu je pro stanovení sazby určující čas započetí dané minuty.

    - Pro hovory delší, než pět minut platí pro každou další započatou minutu nad rámec prvních
      pěti minut snížená sazba 0,20 Kč bez ohledu na dobu kdy telefonní hovor probíhá.

    - V rámci promo akce operátora dále platí, že hovory na nejčastěji volané číslo v rámci výpisu
      nebudou zpoplatněny. V případě, že by výpis obsahoval dvě nebo více takový čísel,
      zpoplatněny nebudou hovory na číslo s aritmeticky nejvyšší hodnotou.
     */
    private BigDecimal getAmount(CallRecord r) {
        // Promo
        BigDecimal amount = BigDecimal.ZERO;
        if (mostCalledNumber.equals(r.number)) {
            return amount;
        }
        int total = (int) Math.ceil(((double) Duration.between(r.from, r.to).getSeconds() / 60));
        int base = Math.min(total, discountedTime);
        int disc = total > discountedTime ? total - discountedTime : 0;

        // count amount for base time
        LocalTime startTime = r.from.toLocalTime();
        for (int i = 0; i < base; i++) {
            amount = amount.add(isInBaseInterval(startTime) ? baseRate : offRate);
            startTime = startTime.plusMinutes(1L);
        }
        // count amount for discounted time
        amount = amount.add(discRate.multiply(new BigDecimal(disc)));
        return amount;
    }

    private boolean isInBaseInterval(LocalTime time) {
        return time.isAfter(baseRateStart) && time.isBefore(baseRateEnd);
    }

    private List<CallRecord> parseLog(String phoneLog) throws ParseException {
        List<CallRecord> result = new ArrayList<>();
        String[] records = phoneLog.replaceAll("\r", "").split("\n");
        for (String r : records) {
            result.add(parse(r));
        }
        return result;
    }

    private CallRecord parse(String r) throws ParseException {
        String[] data = r.split(" ");
        CallRecord record = new CallRecord();
        record.number = Long.parseLong(data[0]);
        record.from = LocalDateTime.parse(data[1] + " " + data[2], formatter);
        record.to = LocalDateTime.parse(data[3] + " " + data[4], formatter);
        return record;
    }

    private Long getMostCalledNumber(List<CallRecord> records) {
        final Map<Long, Integer> numberCount = new HashMap<>();
        for (CallRecord r : records) {
            int count = numberCount.getOrDefault(r.number, 0);
            numberCount.put(r.number, count + 1);
        }
        int max = 0;
        Long number = 0L;
        for (Map.Entry<Long, Integer> entry : numberCount.entrySet()) {
            Long num = entry.getKey();
            Integer count = entry.getValue();
            if ((count > max) || ((count == max) && (num.compareTo(number) > 0))) {
                max = count;
                number = num;
            }
        }
        return number;
    }

    static class CallRecord {
        Long number;
        LocalDateTime from;
        LocalDateTime to;
    }


}
