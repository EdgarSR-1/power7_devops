// package com.example;

public class Test {
    int a = 10;

    public void display() {
        System.out.println("Value of a: " + a);
    }

    public static void main(String[] args) {
        System.out.println("Hello, World!");

        for (int i = 0; i < args.length; i++){
            System.out.println("Argument " + i + ": " + args[i]);
        }

        Test obj = new Test();
        obj.display();
    }
}