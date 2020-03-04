package edu.dartmouth.cs.myrunscollector;

import android.util.Log;

// sliding window to hold onto a window and sum of accelerometer data
public class EvictList {
    private class Node {
        public Node previous;
        public Node next;
        public double value;

        public Node(double value, Node next, Node previous) {
            this.previous = previous;
            this.next = next;
            this.value = value;
        }
    }

    private double summation = 0.0;
    private int size = 0;
    private int maximum;

    private Node head;
    private Node tail;

    public EvictList(int max) {
        this.maximum = max;
        this.head = null;
        this.tail = null;
    }

    // insertion will abide by eviction policy which removes least recently used item
    public void insert(double magnitude) {
        if((this.head == null && this.tail == null) || this.maximum == 1) {
            Node element = new Node(magnitude, null, null);
            this.head = element;
            this.tail = element;
            this.summation = magnitude;
        } else {
            // items already in list
            if(this.size >= this.maximum) {
                // add new element to the list and increment the sum
                this.summation -= tail.value;
                tail = tail.previous;
                tail.next = null;
            } else {
                this.size += 1;
            }

            // add the new element regardless
            Node element = new Node(magnitude, head, null);
            head.previous = element;
            head = element;

            this.summation += magnitude;
        }
    }

    public double getSummation() {
        return this.summation;
    }

    public boolean isFull() {
        return (size >= maximum);
    }

    public double getAverage() {
        if(this.size != 0) {
            return this.summation / this.size;
        }

        return 0;
    }

    public void outputList() {
        while(head != null) {
            Node n = head;
            Log.d("johnmacdonald", "Element: " + String.valueOf(n.value));
            head = head.next;
        }

        Log.d("johnmacdonald", "Summation: " + String.valueOf(this.summation));
    }
}