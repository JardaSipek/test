package com.arbes.test;


import com.phonecompany.billing.TelephoneBillCalculatorImpl;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;


public class App {
    public static void main(String[] args) {
        try {
            File file = new File(App.class.getClassLoader().getResource("PhoneLog.csv").getPath());
            byte[] encoded = Files.readAllBytes(file.toPath());
            String str = new String(encoded, Charset.defaultCharset());
            TelephoneBillCalculatorImpl calculator = new TelephoneBillCalculatorImpl();
            final BigDecimal value = calculator.calculate(str);
            System.out.println(value);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
