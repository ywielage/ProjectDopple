package com.inf2c.doppleapp.conversion;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class DoppleJsonConversion {

    public static void convertCsvToJson(File file) {
        ArrayList<String> list = new ArrayList<>();

        try {
            Scanner scanner = new Scanner(file);
            while(scanner.hasNextLine()) {
                String data = scanner.nextLine();
                list.add(data);
            }
        } catch(Exception e) {
            System.out.println(e);
        }

        for(String s : list) {
            System.out.println("--");
            System.out.println(s);
        }
    }

}
