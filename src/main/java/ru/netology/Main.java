package ru.netology;

public class Main {
  public static void main(String[] args) {
    Thread thread = new Thread(new Server());
    thread.start();
  }
}


