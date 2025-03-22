package org.example.database;

import java.util.ArrayList;

public class MyBlockingQueue<E> {
    private ArrayList<E> q;
    private int capacity = 10;

    public MyBlockingQueue(int cap) {
        this.capacity = cap;
        this.q = new ArrayList<E>(this.capacity);
    }

    boolean isEmpty() {
        return q.isEmpty();
    }

    boolean isFull() {
        return q.size() == capacity;
    }

    int size() { return q.size(); }

    synchronized void add(E e) {
        while (isFull()) {
            try {
                wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        q.add(e);
        notifyAll();
    }

    synchronized E get() {
        while (isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        E item = q.remove(0);
        notifyAll();
        return item;
    }
}